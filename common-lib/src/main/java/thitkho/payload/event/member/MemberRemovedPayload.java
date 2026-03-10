package thitkho.payload.event.member;

import java.time.LocalDateTime;

public record MemberRemovedPayload(
        String roomId,
        String actorUserId,    // Người kick member ra
        String targetUserId  // User bị kic
) {}

