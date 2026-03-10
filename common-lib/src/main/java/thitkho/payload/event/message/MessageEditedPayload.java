package thitkho.payload.event.message;

import java.time.LocalDateTime;

public record MessageEditedPayload(
        String roomId,
        String messageId,
        String newContent,
        boolean edited,
        LocalDateTime updatedAt
) {}
