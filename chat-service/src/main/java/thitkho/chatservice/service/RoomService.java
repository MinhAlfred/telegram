package thitkho.chatservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import thitkho.chatservice.dto.request.CreatePrivateChatRequest;
import thitkho.chatservice.dto.request.CreateRoomRequest;
import thitkho.chatservice.dto.request.UpdateRoomRequest;
import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.model.enums.MemberRole;
import thitkho.payload.CursorPage;

import java.util.List;

public interface RoomService {
    // Room
    RoomResponse createPrivateChatRoom(String userId, String targetUserId);
    RoomResponse createGroupChatRoom(String userId, CreateRoomRequest request);
    RoomResponse getRoom(String userId, String roomId);
    CursorPage<RoomResponse> getMyRooms(String userId, String cursor, int limit);
    RoomResponse updateRoom(String userId, String roomId, UpdateRoomRequest request);
    void deleteRoom(String userId, String roomId);

    // Read state
    void markAsRead(String userId, String roomId);

    // Invite
    RoomResponse joinByInviteCode(String userId, String inviteCode);
    RoomResponse resetInviteCode(String userId, String roomId);
}
