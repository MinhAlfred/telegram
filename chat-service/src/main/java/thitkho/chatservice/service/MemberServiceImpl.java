package thitkho.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import thitkho.chatservice.dto.request.AddMemberRequest;
import thitkho.chatservice.dto.response.RoomMemberResponse;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.model.Room;
import thitkho.chatservice.model.RoomMember;
import thitkho.chatservice.model.enums.MemberRole;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.chatservice.repository.RoomRepository;
import thitkho.exception.AppException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements  MemberService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;

    @Override
    public void addMembers(String userId, String roomId, AddMemberRequest request) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(RoomErrorCode.ROOM_NOT_FOUND));
        validateAdminAccess(userId, roomId);
        LocalDateTime now= LocalDateTime.now();
        Set<String> existingMembers = roomMemberRepository.findByRoomId(roomId)
                .stream()
                .map(RoomMember::getUserId)
                .collect(Collectors.toSet());
        List<RoomMember> newMember = request.userIds().stream()
                        .filter(id -> !existingMembers.contains(id))
                .map(targetUserId -> {
                    RoomMember member = new RoomMember();
                    member.setRoomId(roomId);
                    member.setUserId(targetUserId);
                    member.setRole(MemberRole.MEMBER);
                    member.setJoinedAt(now);
                    member.setLastReadAt(now);
                    return member;
                })
                        .toList();
        if(!newMember.isEmpty()) {
            roomMemberRepository.saveAll(newMember);
        }
    }

    @Override
    public void removeMember(String userId, String roomId, String targetUserId) {

    }

    @Override
    public void leaveRoom(String userId, String roomId) {

    }

    @Override
    public List<RoomMemberResponse> getMembers(String roomId) {
        return List.of();
    }

    @Override
    public void changeMemberRole(String userId, String roomId, String targetUserId, MemberRole role) {

    }

    // Helper methods for permission checks, etc.
    private void validateAdminAccess(String userId, String roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.NOT_A_MEMBER));
        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new AppException(RoomErrorCode.NOT_A_MEMBER);
        }
    }
}
