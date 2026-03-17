package thitkho.payload.event.room;

import java.time.LocalDateTime;
import java.util.List;

public record RoomCreatedPayload(
        String id,
        String name,
        String avatar,
        String description,
        String type,
        String createdBy,
        String lastMessage,
        int memberCount,
        LocalDateTime lastMessageAt,
        LocalDateTime createdAt,
        List<String> memberIds   // dùng để relay push đúng user
) {}

