package thitkho.chatservice.dto.response;

import lombok.Builder;
import thitkho.chatservice.model.enums.MessageType;
import thitkho.dto.response.ReplyPreview;

import java.time.LocalDateTime;
import java.util.List;
@Builder
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
        ReplyPreview replyTo,        // nested message được reply
        List<ReactionResponse> reactions,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}