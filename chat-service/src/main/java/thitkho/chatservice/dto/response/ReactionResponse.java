package thitkho.chatservice.dto.response;

import java.util.List;

public record ReactionResponse(
        String emoji,
        int count,
        List<String> userIds            // ai đã react
) {}