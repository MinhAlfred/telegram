package thitkho.dto.response;


import lombok.Builder;

@Builder
public record ReplyPreview(
        String id,
        String senderId,
        String senderName,
        String content,      // preview ngắn
        String type
) {}