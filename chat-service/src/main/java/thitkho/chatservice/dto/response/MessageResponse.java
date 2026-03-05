package thitkho.chatservice.dto.response;

import thitkho.chatservice.model.enums.MessageType;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        String id,
        String roomId,
        String senderId,
        String senderName,              // lấy từ User-Service
        String senderAvatar,            // lấy từ User-Service
        MessageType type,
        String content,
        String mediaUrl,
        String fileName,
        Long fileSize,
        String replyToId,
        MessageResponse replyTo,        // nested message được reply
        String threadId,
        int replyCount,
        List<ReactionResponse> reactions,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}