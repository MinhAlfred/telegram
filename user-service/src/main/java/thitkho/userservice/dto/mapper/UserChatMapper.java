package thitkho.userservice.dto.mapper;

import org.springframework.stereotype.Component;
import thitkho.dto.OnlineStatus;
import thitkho.dto.response.UserInfoChatResponse;
import thitkho.userservice.model.User;

@Component
public class UserChatMapper {

    public static UserInfoChatResponse toChatResponse(User user) {
        return new UserInfoChatResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatar(),
                user.getDateOfBirth(),
                convertOnlineStatus(user.getOnlineStatus()),
                user.getLastSeen(),
                user.isActive(),
                user.getCreatedAt()
        );
    }

    private static OnlineStatus convertOnlineStatus(thitkho.userservice.model.enums.OnlineStatus status) {
        if (status == null) {
            return OnlineStatus.OFFLINE;
        }
        return OnlineStatus.valueOf(status.name());
    }
}

