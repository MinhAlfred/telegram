package thitkho.chatservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import thitkho.chatservice.dto.request.AddMemberRequest;
import thitkho.chatservice.dto.response.RoomMemberResponse;
import thitkho.chatservice.model.enums.MemberRole;

import java.util.List;

public interface MemberService {
    // Member
    void addMembers(String userId, String roomId, AddMemberRequest request);
    void removeMember(String userId, String roomId, String targetUserId);
    void leaveRoom(String userId, String roomId);
    Page<RoomMemberResponse> getMembers(String roomId, Pageable pageable);
    void changeMemberRole(String userId, String roomId, String targetUserId, MemberRole role);
}
