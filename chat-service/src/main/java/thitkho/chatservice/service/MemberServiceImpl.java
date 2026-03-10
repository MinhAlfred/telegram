package thitkho.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thitkho.chatservice.client.UserClient;
import thitkho.chatservice.dto.mapper.MemberMapper;
import thitkho.chatservice.dto.request.AddMemberRequest;
import thitkho.chatservice.dto.response.RoomMemberResponse;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.model.Room;
import thitkho.chatservice.model.RoomMember;
import thitkho.chatservice.model.enums.MemberRole;
import thitkho.chatservice.producer.ChatEventProducer;
import thitkho.constant.KafkaTopics;
import thitkho.payload.event.ChatEvent;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.chatservice.repository.RoomRepository;
import thitkho.exception.AppException;
import thitkho.dto.response.UserInfoChatResponse;
import thitkho.payload.event.member.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemberServiceImpl implements  MemberService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserClient userClient;
    private final ChatEventProducer chatEventProducer;

    @Override
    @Transactional
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
            roomRepository.incrementMemberCount(roomId, newMember.size());
            publishMemberEvent(
                    roomId,
                    MemberEventType.MEMBER_ADDED,
                    new MemberAddedPayload(
                            roomId,
                            userId,
                            newMember.stream().map(RoomMember::getUserId).toList(),
                            room.getMemberCount() + newMember.size()
                    )
            );
        }
    }

    @Override
    public void removeMember(String userId, String roomId, String targetUserId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(RoomErrorCode.ROOM_NOT_FOUND));
        validateAdminAccess(userId, roomId);
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        roomMemberRepository.delete(member);
        roomRepository.decrementMemberCount(roomId);
        publishMemberEvent(
                roomId,
                MemberEventType.MEMBER_REMOVED,
                new MemberRemovedPayload(
                        roomId,
                        userId,
                        targetUserId
                )
        );
    }

    @Override
    public void leaveRoom(String userId, String roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        roomMemberRepository.delete(member);
        roomRepository.decrementMemberCount(roomId);
        publishMemberEvent(
                roomId,
                MemberEventType.MEMBER_LEFT,
                new MemberLeftPayload(
                        roomId,
                        userId
                )
        );
    }

    @Override
    public Page<RoomMemberResponse> getMembers(String roomId, Pageable pageable) {
        Page<RoomMember> members = roomMemberRepository.findByRoomId(roomId, pageable);
        List<String> userIds = members.stream().map(RoomMember::getUserId).toList();
        Map<String, UserInfoChatResponse> userInfoMap = userClient.getUsersByIds(userIds);
        return members.map(member -> {
            UserInfoChatResponse userInfo = userInfoMap.get(member.getUserId());
            return MemberMapper.toRoomMemberResponse(member,userInfo.displayName(), userInfo.avatar());
        });
    }

    @Override
    public void changeMemberRole(String userId, String roomId, String targetUserId, MemberRole role) {
        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.NOT_A_MEMBER));
        validateAdminAccess(userId, roomId);
        RoomMember targetMember = roomMemberRepository.findByRoomIdAndUserId(roomId, targetUserId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        targetMember.setRole(role);
        roomMemberRepository.save(targetMember);
        publishMemberEvent(
                roomId,
                MemberEventType.MEMBER_ROLE_CHANGED,
                new MemberRoleChangedPayload(
                        roomId,
                        userId,
                        targetUserId,
                        role.name()
                )
        );
    }

    // Helper methods for permission checks, etc.
    private void validateAdminAccess(String userId, String roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.NOT_A_MEMBER));
        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new AppException(RoomErrorCode.NOT_A_MEMBER);
        }
    }

    private void publishMemberEvent(String roomId, MemberEventType eventType, Object payload) {
        chatEventProducer.publish(KafkaTopics.ROOM_EVENTS, roomId, new ChatEvent<>(eventType.name(), roomId, payload));
    }
}
