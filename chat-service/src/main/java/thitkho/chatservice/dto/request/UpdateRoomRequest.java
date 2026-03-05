package thitkho.chatservice.dto.request;

public record UpdateRoomRequest(
        String name,
        String avatar,
        String description
) {}