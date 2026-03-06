package thitkho.chatservice.dto.response;

import lombok.Builder;

import java.util.List;
@Builder
public record ReactionResponse(
        String messageId,
        String emoji,
        long count,
        boolean reactedByMe
) {}