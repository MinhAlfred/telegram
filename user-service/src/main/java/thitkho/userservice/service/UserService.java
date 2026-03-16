package thitkho.userservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import thitkho.userservice.dto.request.ChangePasswordRequest;
import thitkho.userservice.dto.request.LoginRequest;
import thitkho.userservice.dto.request.RegisterRequest;
import thitkho.userservice.dto.request.UpdateProfileRequest;
import thitkho.userservice.dto.response.AuthResponse;
import thitkho.userservice.dto.response.OnlineStatusResponse;
import thitkho.userservice.dto.response.UserResponse;
import thitkho.userservice.model.enums.Role;

import java.io.IOException;
import java.util.List;

public interface UserService {

    // Auth
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse loginGoogle(String googleToken);
    AuthResponse refreshToken(String refreshToken);
    void logout(String token);

    // Profile
    UserResponse getProfile(String userId);
    UserResponse updateProfile(String userId, UpdateProfileRequest request);
    UserResponse updateAvatar(String userId, MultipartFile file) throws IOException;
    void changePassword(String userId, ChangePasswordRequest request);

    // Online Status
    void setOnline(String userId);
    void setOffline(String userId);
    OnlineStatusResponse getOnlineStatus(String userId);
    List<OnlineStatusResponse> getOnlineStatusByIds(List<String> userIds);

    // Admin
    Page<UserResponse> getAllUsers(Pageable pageable);
    void banUser(String userId);
    void unbanUser(String userId);
    void changeRole(String userId, Role role);

    Page<UserResponse> search(Pageable pageable,String query);
}