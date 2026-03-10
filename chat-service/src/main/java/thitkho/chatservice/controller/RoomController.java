package thitkho.chatservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import thitkho.chatservice.dto.request.CreateRoomRequest;
import thitkho.chatservice.dto.request.UpdateRoomRequest;
import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.service.RoomService;
import thitkho.payload.ApiResponse;
import thitkho.payload.CursorPage;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Management", description = "APIs for managing chat rooms")
public class RoomController {
    private final RoomService roomService;

    @PostMapping("/private")
    @Operation(description = "Create or get existing private chat room between two users. Returns existing room if already created.")
    public ApiResponse<RoomResponse> createPrivateRoom(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String targetUserId) {
        return ApiResponse.success(roomService.createPrivateChatRoom(userId, targetUserId));
    }

    @PostMapping("/group")
    @Operation(description = "Create a new group chat room with multiple members. Creator becomes room owner.")
    public ApiResponse<RoomResponse> createGroupRoom(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CreateRoomRequest request) {
        return ApiResponse.success(roomService.createGroupChatRoom(userId, request));
    }

    @GetMapping("/{roomId}")
    @Operation(description = "Get detailed information about a specific room. User must be a member of the room.")
    public ApiResponse<RoomResponse> getRoom(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId) {
        return ApiResponse.success(roomService.getRoom(userId, roomId));
    }

    @GetMapping
    @Operation(description = "Get paginated list of rooms that user is a member of. Sorted by last message time.")
    public ApiResponse<CursorPage<RoomResponse>> getMyRooms(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.success(roomService.getMyRooms(userId, cursor, limit));
    }

    @PutMapping("/{roomId}")
    @Operation(description = "Update room information (name, avatar, description). Only admins or owner can update.")
    public ApiResponse<RoomResponse> updateRoom(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId,
            @RequestBody UpdateRoomRequest request) {
        return ApiResponse.success(roomService.updateRoom(userId, roomId, request));
    }

    @DeleteMapping("/{roomId}")
    @Operation(description = "Soft delete a room by marking it as inactive. Only room owner can delete room.")
    public ApiResponse<Void> deleteRoom(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId) {
        roomService.deleteRoom(userId, roomId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{roomId}/read")
    @Operation(description = "Mark all messages in room as read. Resets unread count to 0.")
    public ApiResponse<Void> markAsRead(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId) {
        roomService.markAsRead(userId, roomId);
        return ApiResponse.success(null);
    }

    @PostMapping("/join")
    @Operation(description = "Join a room using invite code. Returns room information after successful join.")
    public ApiResponse<RoomResponse> joinByInviteCode(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String inviteCode) {
        return ApiResponse.success(roomService.joinByInviteCode(userId, inviteCode));
    }

    @PostMapping("/{roomId}/invite-code/reset")
    @Operation(description = "Generate new invite code for room. Only admins or owner can reset invite code.")
    public ApiResponse<RoomResponse> resetInviteCode(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId) {
        return ApiResponse.success(roomService.resetInviteCode(userId, roomId));
    }
}






