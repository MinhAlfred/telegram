package thitkho.userservice.dto.mapper;

import org.springframework.stereotype.Component;
import thitkho.userservice.dto.response.UserResponse;
import thitkho.userservice.model.User;

@Component
public class UserMapper {

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getPhoneNumber(),
                user.getDateOfBirth(),
                user.getProvider(),
                user.getRole(),
                user.getOnlineStatus(),
                user.getLastSeen(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}