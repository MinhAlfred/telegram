package thitkho.payload.event.member;

import java.time.LocalDateTime;

public record MemberLeftPayload(
        String roomId,
        String userId
) {}

