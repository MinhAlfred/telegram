package thitkho.payload.event.room;

import java.time.LocalDateTime;

public record RoomCreatedPayload(
        String id,
        String name,
        String avatar,
        String description,
        String type,
        String createdBy,
        String lastMessage,    // tin nhắn mới nhất
        int memberCount,       // số lượng thành viên hiện tại
        LocalDateTime lastMessageAt, // thời gian tin nhắn mới nhất
        LocalDateTime createdAt
) {}

