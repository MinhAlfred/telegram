package thitkho.chatservice.dto.response;

import lombok.Builder;
import thitkho.chatservice.model.enums.RoomType;

import java.time.LocalDateTime;
@Builder
public record RoomResponse(
        String id,
        String name,
        String avatar,
        String description,
        RoomType type,
        String createdBy,
        String lastMessage,    // tin nhắn mới nhất
        LocalDateTime lastMessageAt, // thời gian tin nhắn mới nhất
        long unreadCount,               // số tin chưa đọc
        LocalDateTime createdAt
) {}