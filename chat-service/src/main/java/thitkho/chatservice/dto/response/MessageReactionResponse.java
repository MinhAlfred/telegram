package thitkho.chatservice.dto.response;

import lombok.Builder;

@Builder
public record MessageReactionResponse(
        String messageId,
        String emoji
) {
}
