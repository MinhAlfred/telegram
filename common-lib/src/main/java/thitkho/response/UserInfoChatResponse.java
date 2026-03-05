package thitkho.response;

import thitkho.enums.OnlineStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserInfoChatResponse(
        String id,
        String username,
        String email,
        String displayName,
        String avatar,
        LocalDate dateOfBirth,
        OnlineStatus onlineStatus,
        LocalDateTime lastSeen,
        boolean isActive,
        LocalDateTime createdAt
) {
}