package thitkho.chatservice.dto.mapper;

import org.springframework.stereotype.Component;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.dto.response.ReactionResponse;
import thitkho.chatservice.dto.response.ReplyPreview;
import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.model.Message;
import thitkho.chatservice.model.MessageReaction;
import thitkho.chatservice.model.Room;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatMapper {
    public static MessageResponse toMessageResponse(Message message, String senderName, String senderAvt , List<ReactionResponse> reactions, ReplyPreview replyPreview) {
        return MessageResponse.builder()
                .id(message.getId())
                .roomId(message.getRoomId())
                .content(message.getContent())
                .fileSize(message.getFileSize())
                .mediaUrl(message.getMediaUrl())
                .replyToId(message.getReplyToId())
                .fileName(message.getFileName())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .type(message.getType())
                .isDeleted(message.isDeleted())
                .reactions(reactions)
                .senderId(message.getSenderId())
                .senderName(senderName)
                .senderAvatar(senderAvt)
                .replyTo(replyPreview)
                .build();
    }
    public static ReplyPreview toReplyPreview(Message message, String senderName) {
        return ReplyPreview.builder()
                .id(message.getId())
                .content(message.getContent())
                .senderId(message.getSenderId())
                .type(message.getType())
                .senderName(senderName)
                .build();
    }
//     public static ReactionResponse toReactionResponse(MessageReaction reaction) {
//        return ReactionResponse.builder()
//                .
//                .build();
//    }
    public static List<ReactionResponse> mapReactions(
        String messageId,
        Map<String, Long> summary, // Giả sử Jackson đã map JSONB về Map<String, Long>
        String userEmoji           // Emoji mà tôi đã thả, null nếu chưa thả gì
        ) {
        if (summary == null || summary.isEmpty()) {
        return Collections.emptyList();
        }

    return summary.entrySet().stream()
            .map(entry -> new ReactionResponse(
                    messageId,
                    entry.getKey(),               // Emoji (e.g., "❤️")
                    entry.getValue(),              // Tổng số người thả (e.g., 10)
                    entry.getKey().equals(userEmoji) // So sánh xem tôi có thả cái này không
            ))
            .toList();
}

}