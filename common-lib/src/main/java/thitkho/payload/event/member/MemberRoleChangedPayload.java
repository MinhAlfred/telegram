package thitkho.payload.event.member;

import java.time.LocalDateTime;

public record MemberRoleChangedPayload(
        String roomId,
        String actorUserId,    // Người thay đổi role
        String targetUserId,   // User được thay đổi role
        String newRole        // Role mới (OWNER, ADMIN, MEMBER)
) {}

