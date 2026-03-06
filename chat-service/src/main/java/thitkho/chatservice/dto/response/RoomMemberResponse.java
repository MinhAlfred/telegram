package thitkho.chatservice.dto.response;

import lombok.Builder;
import thitkho.chatservice.model.enums.MemberRole;

import java.time.LocalDateTime;
@Builder
public record RoomMemberResponse(
        String id,
        String roomId,
        String userId,
        String displayName,             // lấy từ User-Service
        String avatar,                  // lấy từ User-Service
        MemberRole role,
        LocalDateTime joinedAt,
        LocalDateTime lastReadAt
) {}