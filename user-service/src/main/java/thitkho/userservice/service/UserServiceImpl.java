package thitkho.userservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.util.Pair;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import thitkho.exception.AppException;
import thitkho.userservice.dto.mapper.UserMapper;
import thitkho.userservice.dto.request.ChangePasswordRequest;
import thitkho.userservice.dto.request.LoginRequest;
import thitkho.userservice.dto.request.RegisterRequest;
import thitkho.userservice.dto.request.UpdateProfileRequest;
import thitkho.userservice.dto.response.AuthResponse;
import thitkho.userservice.dto.response.GoogleUserInfo;
import thitkho.userservice.dto.response.OnlineStatusResponse;
import thitkho.userservice.dto.response.UserResponse;
import thitkho.userservice.exception.AuthErrorCode;
import thitkho.userservice.model.User;
import thitkho.userservice.model.enums.AuthProvider;
import thitkho.userservice.model.enums.OnlineStatus;
import thitkho.userservice.model.enums.Role;
import thitkho.userservice.repository.UserRepository;
import thitkho.userservice.util.CloudinaryUtils;
import thitkho.userservice.util.GoogleTokenVerifier;
import thitkho.userservice.util.JwtUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final CloudinaryUtils cloudinaryService;
    private final RedisTemplate<String,String> redisTemplate;
    private final GoogleTokenVerifier googleTokenVerifier;
    @Override
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email()))
            throw new AppException(AuthErrorCode.EMAIL_EXISTED);
        if (userRepository.existsByUsername(request.username()))
            throw new AppException(AuthErrorCode.USERNAME_EXISTED);

        User user = User.builder()
                .email(request.email())
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .displayName(request.displayName())
                .provider(AuthProvider.LOCAL)
                .role(Role.USER)
                .onlineStatus(OnlineStatus.OFFLINE)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND));

        if (!user.isActive())
            throw new AppException(AuthErrorCode.USER_BANNED);

        if (!passwordEncoder.matches(request.password(), user.getPassword()))
            throw new AppException(AuthErrorCode.WRONG_PASSWORD);

        return generateAuthResponse(user);
    }

    @Override
    public AuthResponse loginGoogle(String googleToken) {
        GoogleUserInfo googleInfo = googleTokenVerifier.verify(googleToken);
        User user = userRepository
                .findByProviderIdAndProvider(googleInfo.getSubject(), AuthProvider.GOOGLE)
                .orElseGet(() -> {
                    // Tạo account mới nếu chưa có
                    User newUser = User.builder()
                            .email(googleInfo.getEmail())
                            .displayName(googleInfo.getName())
                            .avatar(googleInfo.getPicture())
                            .provider(AuthProvider.GOOGLE)
                            .providerId(googleInfo.getSubject())
                            .role(Role.USER)
                            .onlineStatus(OnlineStatus.OFFLINE)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    return userRepository.save(newUser);
                });

        if (!user.isActive())
            throw new AppException(AuthErrorCode.USER_BANNED);

        return generateAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtils.isValidRefreshToken(refreshToken))
            throw new AppException(AuthErrorCode.INVALID_CREDENTIALS);

        String userId = jwtUtils.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND));

        return generateAuthResponse(user);
    }

    @Override
    public void logout(String token) {
        // Blacklist token vào Redis với TTL = thời gian còn lại của token
        long expiration = jwtUtils.getExpiration(token);
        redisTemplate.opsForValue().set(
                "blacklist:" + token,
                "true",
                expiration,
                TimeUnit.MILLISECONDS
        );
    }


    @Override
    public UserResponse getProfile(String userId) {
        User user = findUserById(userId);
        return UserMapper.toResponse(user);
    }

    @Override
    public UserResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        if (request.displayName() != null)
            user.setDisplayName(request.displayName());
        if (request.phoneNumber() != null)
            user.setPhoneNumber(request.phoneNumber());
        if (request.dateOfBirth() != null)
            user.setDateOfBirth(request.dateOfBirth());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateAvatar(String userId, MultipartFile file) throws IOException {
        User user = findUserById(userId);
        Pair<String,String> avatarUrl = cloudinaryService.uploadMultipartFile(file, "/avatar/" + userId);
        user.setAvatar(avatarUrl.getFirst());
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUserById(userId);
        if (!passwordEncoder.matches(request.oldPassword(), user.getPassword()))
            throw new AppException(AuthErrorCode.WRONG_PASSWORD,"The old password is incorrect");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    public void setOnline(String userId) {
        User user = findUserById(userId);
        user.setOnlineStatus(OnlineStatus.ONLINE);
        userRepository.save(user);

        // Cache vào Redis
        redisTemplate.opsForValue().set("online:" + userId, "true");
    }

    @Override
    public void setOffline(String userId) {
        User user = findUserById(userId);
        user.setOnlineStatus(OnlineStatus.OFFLINE);
        user.setLastSeen(LocalDateTime.now());
        userRepository.save(user);

        // Xóa khỏi Redis
        redisTemplate.delete("online:" + userId);
    }

    @Override
    public OnlineStatusResponse getOnlineStatus(String userId) {
        // Check Redis trước cho nhanh
        Boolean isOnline = redisTemplate.hasKey("online:" + userId);
        return OnlineStatusResponse.builder()
                .userId(userId)
                .isOnline(Boolean.TRUE.equals(isOnline))
                .build();
    }

    @Override
    public List<OnlineStatusResponse> getOnlineStatusByIds(List<String> userIds) {
        return userIds.stream()
                .filter(id -> {
                    Boolean isOnline = redisTemplate.hasKey("online:" + id);
                    return Boolean.TRUE.equals(isOnline);
                })
                .map(id -> OnlineStatusResponse.builder()
                        .userId(id)
                        .isOnline(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(UserMapper::toResponse);
    }

    @Override
    public void banUser(String userId) {
        User user = findUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    @Override
    public void unbanUser(String userId) {
        User user = findUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }

    @Override
    public void changeRole(String userId, Role role) {
        User user = findUserById(userId);
        user.setRole(role);
        userRepository.save(user);
    }

    // ==================== HELPER ====================

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND));
    }

    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(UserMapper.toResponse(user))
                .build();
    }
}
