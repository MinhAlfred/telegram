package thitkho.payload.event.message;

import thitkho.dto.response.ReplyPreview;

import java.time.LocalDateTime;
import java.util.List;

public record MessageSentPayload(
        String id,
        String roomId,
        String senderId,
        String senderName,              // lấy từ User-Service
        String senderAvatar,            // lấy từ User-Service
        String type,
        String content,
        String mediaUrl,
        String fileName,
        Long fileSize,
        String replyToId,
        ReplyPreview replyTo,        // nested message được reply
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}

