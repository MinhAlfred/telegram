package thitkho.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thitkho.chatservice.annotation.RequireRoomMember;
import thitkho.chatservice.client.UserClient;
import thitkho.chatservice.dto.mapper.RoomMapper;
import thitkho.chatservice.dto.request.CreateRoomRequest;
import thitkho.chatservice.dto.request.UpdateRoomRequest;
import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.model.Room;
import thitkho.chatservice.model.RoomMember;
import thitkho.chatservice.model.enums.MemberRole;
import thitkho.chatservice.model.enums.RoomType;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.chatservice.repository.RoomRepository;
import thitkho.exception.AppException;
import thitkho.payload.CursorPage;
import thitkho.dto.response.UserInfoChatResponse;
import thitkho.chatservice.producer.ChatEventProducer;
import thitkho.constant.KafkaTopics;
import thitkho.payload.event.ChatEvent;
import thitkho.payload.event.room.RoomCreatedPayload;
import thitkho.payload.event.room.RoomDeletedPayload;
import thitkho.payload.event.room.RoomEventType;
import thitkho.payload.event.room.RoomReadPayload;
import thitkho.payload.event.room.RoomUpdatedPayload;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class RoomServiceImpl implements  RoomService {
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final UserClient userClient;
    private final ChatEventProducer chatEventProducer;

    @Override
    @Transactional
    public RoomResponse createPrivateChatRoom(String userId, String targetUserId) {
        UserInfoChatResponse targetUserInfo = userClient.getUserById(targetUserId);
        return findExistingPrivateRoom(userId, targetUserId)
                .map(room -> {
                    int unread = roomMemberRepository.findByRoomIdAndUserId(room.getId(), userId)
                            .map(RoomMember::getUnreadCount)
                            .orElse(0);
                    return RoomMapper.toDirectRoomResponse(room, targetUserInfo, unread);
                })
                .orElseGet(() -> {
                    Room room = createBaseRoom(targetUserInfo.displayName(), RoomType.DIRECT, userId,2);
                    saveMembers(room.getId(), List.of(userId, targetUserId), false);
                    String name   = targetUserInfo.displayName();
                    String avatar = targetUserInfo.avatar();
                    publishRoomEvent(
                            room.getId(),
                            RoomEventType.ROOM_CREATED,
                            new RoomCreatedPayload(
                                    room.getId(),
                                    name,
                                    avatar,
                                    null,                               // DIRECT không có description
                                    room.getType().toString(),
                                    room.getCreatedBy(),
                                    room.getLastMessageContent(),
                                    room.getMemberCount(),
                                    room.getLastMessageAt(),                                  // unreadCount — xem note bên dưới
                                    room.getCreatedAt()
                            )
                    );
                    return RoomMapper.toDirectRoomResponse(room, targetUserInfo,0);
                });
    }

    @Override
    @Transactional
    public RoomResponse createGroupChatRoom(String userId, CreateRoomRequest request) {
        UserInfoChatResponse userInfo = userClient.getUserById(userId);
        int count = request.memberIds().size() + 1;
        Room room = createBaseRoom(request.name(), RoomType.GROUP, userId,count);
        saveMembers(room.getId(), List.of(userId), true); // owner
        saveMembers(room.getId(), request.memberIds(), false); // members
        publishRoomEvent(
                room.getId(),
                RoomEventType.ROOM_CREATED,
                new RoomCreatedPayload(
                        room.getId(),
                        request.name(),
                        request.avatar(),
                        null,                               // DIRECT không có description
                        room.getType().toString(),
                        room.getCreatedBy(),
                        room.getLastMessageContent(),
                        room.getMemberCount(),
                        room.getLastMessageAt(),                                  // unreadCount — xem note bên dưới
                        room.getCreatedAt()
                )
        );
        return RoomMapper.toGroupRoomResponse(room, userInfo,0);
    }

    @Override
    @RequireRoomMember
    public RoomResponse getRoom(String userId, String roomId) {
            Room room = findRoomById(roomId);
            RoomMember me = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
            if(room.getType() == RoomType.DIRECT) {
                List<RoomMember> members = roomMemberRepository.findByRoomId(roomId);
                String targetUserId = members.stream()
                        .map(RoomMember::getUserId)
                        .filter(id -> !id.equals(userId))
                        .findFirst()
                        .orElse(userId);
                UserInfoChatResponse targetInfo = userClient.getUserById(targetUserId);
                return RoomMapper.toDirectRoomResponse(room, targetInfo,me.getUnreadCount());
            }
            UserInfoChatResponse user = userClient.getUserById(room.getLastMessageSenderId());
            if(user == null){
                throw new AppException(RoomErrorCode.ROOM_NOT_FOUND,"Room last message sender not found");
            }
            return RoomMapper.toGroupRoomResponse(room, user,me.getUnreadCount());
    }

    @Override
    public CursorPage<RoomResponse> getMyRooms(String userId, String cursor, int limit) {
        LocalDateTime cursorTime = cursor != null
                ? LocalDateTime.parse(cursor)
                : null;
        List<Room> rooms = roomRepository.findRoomsByUserIdWithCursor(userId, cursorTime, limit);
        if(rooms.isEmpty()) {
            return CursorPage.of(List.of(), limit,null);
        }
        Set<String> allNeededUserIds = new HashSet<>();
        List<String> roomTargetId = rooms.stream()
                .filter(r -> r.getType() == RoomType.DIRECT)
                .map(Room::getId)
                .toList();
        Map<String, List<RoomMember>> roomIdToMember = roomMemberRepository.findAllByRoomIdIn(roomTargetId).stream()
                .collect(Collectors.groupingBy(RoomMember::getRoomId));

        Map<String,String> roomIdToTargetUserId = new HashMap<>();
        rooms.forEach(room -> {
            if(room.getType() == RoomType.DIRECT) {
                List<RoomMember> members = roomIdToMember.getOrDefault(room.getId(), List.of());
                String targetUserId = members.stream()
                        .map(RoomMember::getUserId)
                        .filter(id -> !id.equals(userId))
                        .findFirst()
                        .orElse(userId);
                roomIdToTargetUserId.put(room.getId(), targetUserId);
                allNeededUserIds.add(targetUserId);
            }
            if(room.getLastMessageSenderId()!=null){
                allNeededUserIds.add(room.getLastMessageSenderId());
            }
        });
        Map<String,UserInfoChatResponse> userIdToInfo = userClient.getUsersByIds(new ArrayList<>(allNeededUserIds));
        // 3. Map vào Response
        List<RoomResponse> responses = rooms.stream().map(room -> {
            RoomMember me = roomIdToMember.get(room.getId()).stream()
                    .filter(m -> m.getUserId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
            if(room.getType() == RoomType.DIRECT) {
                String target = roomIdToTargetUserId.get(room.getId());
                UserInfoChatResponse targetInfo = userIdToInfo.get(target);
                return RoomMapper.toDirectRoomResponse(room, targetInfo,me.getUnreadCount());
            }
            UserInfoChatResponse senderInfo = userIdToInfo.get(room.getLastMessageSenderId());
            return RoomMapper.toGroupRoomResponse(room, senderInfo,me.getUnreadCount());
        }).toList();
        return CursorPage.of(
                responses,
                limit,
                r -> r.lastMessageAt().toString()   // extract cursor từ item cuối
        );

    }

    @Override
    @Transactional
    public RoomResponse updateRoom(String userId, String roomId, UpdateRoomRequest request) {
        validateAdminAccess(userId, roomId);
        Room room = findRoomById(roomId);
        if (request.name() != null) room.setName(request.name());
        if (request.avatar() != null) room.setAvatar(request.avatar());
        if (request.description() != null) room.setDescription(request.description());
        roomRepository.save(room);
        publishRoomEvent(
                roomId,
                RoomEventType.ROOM_UPDATED,
                new RoomUpdatedPayload(
                        roomId,
                        room.getName(),
                        room.getAvatar(),
                        room.getDescription()
                )
        );
        UserInfoChatResponse senderInfo = null;
        if (room.getLastMessageSenderId() != null) {
            senderInfo = userClient.getUserById(room.getLastMessageSenderId());
        }
        RoomMember me = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        return RoomMapper.toGroupRoomResponse(room, senderInfo, me.getUnreadCount());
    }

    @Override
    @Transactional
    public void deleteRoom(String userId, String roomId) {
        validateOwnerAccess(userId, roomId);
        Room room = findRoomById(roomId);
        room.setActive(false);
        roomRepository.save(room);
        publishRoomEvent(
                roomId,
                RoomEventType.ROOM_DELETED,
                new RoomDeletedPayload(
                        roomId
                )
        );
    }

    @Override
    public void markAsRead(String userId, String roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        member.setUnreadCount(0);
        member.setLastReadAt(LocalDateTime.now());
        roomMemberRepository.save(member);
        publishRoomEvent(
                roomId,
                RoomEventType.ROOM_READ,
                new RoomReadPayload(
                        roomId,
                        userId
                )
        );
    }

    @Override
    public RoomResponse joinByInviteCode(String userId, String inviteCode) {
        return null;
    }

    @Override
    public RoomResponse resetInviteCode(String userId, String roomId) {
        return null;
    }

    // --- Helper Methods ---
    private Room createBaseRoom(String name, RoomType type, String creatorId,int memberCount) {
        String content=null;
        String creator = null;
        LocalDateTime now = null;
        if(type == RoomType.GROUP) {
            content = "Room created by: "+name;
            creator = creatorId;
            now = LocalDateTime.now();
        }
        Room room = Room.builder()
                .name(name)
                .type(type)
                .createdBy(creatorId)
                .isActive(true)
                .lastMessageSenderId(creator)
                .lastMessageContent(content)
                .lastMessageAt(now)
                .memberCount(memberCount)
                .build();
        return roomRepository.save(room);
    }
    private void saveMembers(String roomId, List<String> userIds, boolean isOwner) {
        MemberRole role = isOwner ? MemberRole.OWNER : MemberRole.MEMBER;
        userIds.forEach(userId -> {
            LocalDateTime now = LocalDateTime.now();
            RoomMember member = RoomMember.builder()
                    .roomId(roomId)
                    .userId(userId)
                    .role(role)
                    .joinedAt(now)
                    .lastReadAt(now)
                    .unreadCount(0)
                    .build();
            roomMemberRepository.save(member);
        });
    }

    private Optional<Room> findExistingPrivateRoom(String userId1, String userId2) {
        Optional<Room> existRoom = roomRepository.findPrivateRoom(userId1, userId2);
        if(existRoom.isEmpty()){
            log.info("No existing private room found for users {} and {}", userId1, userId2);
        } else {
            log.info("Found existing private room {} for users {} and {}", existRoom.get().getId(), userId1, userId2);
        }
        return existRoom;
    }

    private Room findRoomById(String roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new AppException(RoomErrorCode.ROOM_NOT_FOUND));
    }

    private void validateAdminAccess(String userId, String roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.NOT_A_MEMBER));
        if (member.getRole() != MemberRole.OWNER && member.getRole() != MemberRole.ADMIN) {
            throw new AppException(RoomErrorCode.NOT_A_MEMBER);
        }
    }

    private void validateOwnerAccess(String userId, String roomId) {
        RoomMember member = roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.NOT_A_MEMBER));

        if (member.getRole() != MemberRole.OWNER) {
            throw new AppException(RoomErrorCode.NOT_A_MEMBER);
        }
    }

    private void publishRoomEvent(String roomId, RoomEventType eventType, Object payload) {
        String topic = switch (eventType) {
            case ROOM_CREATED, ROOM_DELETED, ROOM_UPDATED -> KafkaTopics.ROOM_METADATA;
            default -> KafkaTopics.ROOM_EVENTS;
        };

        chatEventProducer.publish(topic, roomId, new ChatEvent<>(eventType.name(), roomId, payload));
    }
}
