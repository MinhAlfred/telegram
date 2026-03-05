package thitkho.chatservice.dto.response;

public record UnreadCountResponse(
        String roomId,
        long unreadCount
) {}
