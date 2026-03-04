package thitkho.userservice.dto.response;

import lombok.Builder;

import java.time.LocalDateTime;
@Builder
public record OnlineStatusResponse(
        String userId,
        boolean isOnline,
        LocalDateTime lastSeen
) {}