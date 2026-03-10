package thitkho.payload.event.message;

import java.time.LocalDateTime;

public record MessageDeletedPayload(
        String roomId,
        String messageId,
        boolean deleted,       // true
        LocalDateTime occurredAt
) {}

