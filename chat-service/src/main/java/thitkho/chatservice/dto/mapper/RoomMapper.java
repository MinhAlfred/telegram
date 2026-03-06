package thitkho.chatservice.dto.mapper;

import org.springframework.stereotype.Component;
import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.model.Room;
import thitkho.response.UserInfoChatResponse;
@Component
public class RoomMapper {
    public RoomResponse toDirectRoomResponse(Room room, UserInfoChatResponse targetInfo, int unreadCount) {
        String name   = targetInfo != null ? targetInfo.displayName() : "Unknown";
        String avatar = targetInfo != null ? targetInfo.avatar()   : null;
        return new RoomResponse(
                room.getId(),
                name,
                avatar,
                null,                               // DIRECT không có description
                room.getType(),
                room.getCreatedBy(),
                room.getLastMessageContent(),
                room.getLastMessageAt(),
                unreadCount,                                  // unreadCount — xem note bên dưới
                room.getCreatedAt()
        );
    }
    public RoomResponse toGroupRoomResponse(Room room, UserInfoChatResponse senderInfo, int unreadCount) {
        String senderName =room.getLastMessageContent() == null?"Room created by "+ senderInfo.displayName(): senderInfo.displayName()+": "+room.getLastMessageContent();
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getAvatar(),
                room.getDescription(),
                room.getType(),
                room.getCreatedBy(),                                // memberCount — xem note bên dưới
                senderName,
                room.getLastMessageAt(),
                unreadCount,                                  // unreadCount — xem note bên dưới
                room.getCreatedAt()
        );
    }

}
