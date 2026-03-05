package thitkho.chatservice.dto.mapper;

import org.springframework.stereotype.Component;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.dto.response.ReactionResponse;
import thitkho.chatservice.dto.response.RoomResponse;
import thitkho.chatservice.model.Message;
import thitkho.chatservice.model.MessageReaction;
import thitkho.chatservice.model.Room;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ChatMapper {

    public static RoomResponse toRoomResponse(Room room,
                                              int memberCount,
                                              MessageResponse lastMessage,
                                              long unreadCount) {
        return new RoomResponse(
                room.getId(),
                room.getName(),
                room.getAvatar(),
                room.getDescription(),
                room.getType(),
                room.getCreatedBy(),
                memberCount,
                lastMessage,
                unreadCount,
                room.getCreatedAt()
        );
    }

    public static MessageResponse toMessageResponse(Message message,
                                                    String senderName,
                                                    String senderAvatar,
                                                    MessageResponse replyTo,
                                                    List<ReactionResponse> reactions) {
        return new MessageResponse(
                message.getId(),
                message.getRoomId(),
                message.getSenderId(),
                senderName,
                senderAvatar,
                message.getType(),
                message.isDeleted() ? null : message.getContent(),
                message.isDeleted() ? null : message.getMediaUrl(),
                message.isDeleted() ? null : message.getFileName(),
                message.isDeleted() ? null : message.getFileSize(),
                message.getReplyToId(),
                replyTo,
                message.getThreadId(),
                message.getReplyCount(),
                reactions,
                message.isDeleted(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    public static ReactionResponse toReactionResponse(String emoji,
                                                      List<MessageReaction> reactions) {
        return new ReactionResponse(
                emoji,
                reactions.size(),
                reactions.stream()
                        .map(MessageReaction::getUserId)
                        .collect(Collectors.toList())
        );
    }
}