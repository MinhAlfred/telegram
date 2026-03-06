package thitkho.chatservice.dto.mapper;

import org.springframework.stereotype.Component;
import thitkho.chatservice.dto.response.RoomMemberResponse;
import thitkho.chatservice.model.RoomMember;

public class MemberMapper {
    public static RoomMemberResponse toRoomMemberResponse(RoomMember member, String displayName, String avatarUrl) {
        return RoomMemberResponse.builder()
                .roomId(member.getRoomId())
                .userId(member.getUserId())
                .role(member.getRole())
                .displayName(displayName)
                .joinedAt(member.getJoinedAt())
                .avatar(avatarUrl)
                .lastReadAt(member.getLastReadAt())
                .id(member.getId())
                .build();
    }
}
