package thitkho.chatservice.dto.response;


import lombok.Builder;
import thitkho.chatservice.model.enums.MessageType;
@Builder
public record ReplyPreview(
        String id,
        String senderId,
        String senderName,
        String content,      // preview ngắn
        MessageType type
) {}