package thitkho.chatservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import thitkho.chatservice.model.enums.MessageType;

public record SendMessageRequest(
        @NotBlank String roomId,
        @NotNull MessageType type,
        String content,             // required nếu type = TEXT
        String mediaUrl,            // required nếu type = IMAGE/FILE
        String fileName,
        Long fileSize,
        String replyToId           // null nếu không reply
) {}