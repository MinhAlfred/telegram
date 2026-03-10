package thitkho.payload.event.member;

import java.time.LocalDateTime;
import java.util.List;

public record MemberAddedPayload(
        String roomId,
        String actorUserId,      // Người thêm member
        List<String> memberIds,  // Danh sách ID user được thêm vào
        int memberCount       // Tổng số member sau khi thêm
) {}

