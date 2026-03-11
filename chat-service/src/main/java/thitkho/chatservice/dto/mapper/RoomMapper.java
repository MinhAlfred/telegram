package thitkho.chatservice.dto.mapper;

import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.model.Room;
import thitkho.dto.response.UserInfoChatResponse;
public class RoomMapper {
    public static RoomResponse toDirectRoomResponse(Room room, UserInfoChatResponse targetInfo, int unreadCount, boolean isNew) {
        String name   = targetInfo != null ? targetInfo.displayName() : "Unknown";
        String avatar = targetInfo != null ? targetInfo.avatar()   : null;
        return new RoomResponse(
                room.getId(),
                name,
                avatar,
                null,
                room.getType(),
                room.getCreatedBy(),
                room.getLastMessageContent(),
                room.getLastMessageAt(),
                unreadCount,
                room.getMemberCount(),
                room.getCreatedAt(),
                isNew
        );
    }
    public static RoomResponse toGroupRoomResponse(Room room, UserInfoChatResponse senderInfo, int unreadCount) {
        String senderName = room.getLastMessageContent() == null ? null : room.getLastMessageContent();
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getAvatar(),
                room.getDescription(),
                room.getType(),
                room.getCreatedBy(),
                senderName,
                room.getLastMessageAt(),
                unreadCount,
                room.getMemberCount(),
                room.getCreatedAt(),
                false
        );
    }
}
