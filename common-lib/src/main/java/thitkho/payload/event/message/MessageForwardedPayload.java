package thitkho.payload.event.message;

import java.time.LocalDateTime;

public record MessageForwardedPayload(
        String id,
        String roomId,
        String senderId,
        String senderName,
        String senderAvatar,
        String type,
        String content,
        String mediaUrl,
        String fileName,
        Long fileSize,
        boolean forwarded,       // true
        LocalDateTime occurredAt
) {}

