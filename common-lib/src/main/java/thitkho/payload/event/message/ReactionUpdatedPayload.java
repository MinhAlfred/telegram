package thitkho.payload.event.message;

import java.time.LocalDateTime;
import java.util.Map;

public record ReactionUpdatedPayload(
        String roomId,
        String messageId,
        Map<String, Long> reactionSummary,  // {"LIKE": 10, "HEART": 5}
        String reactorUserId,               // userId vừa react
        String reactorEmoji,
        boolean removed
) {}

