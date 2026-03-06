package thitkho.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import thitkho.chatservice.client.UserClient;
import thitkho.chatservice.dto.mapper.ChatMapper;
import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.request.UpdateLastReadRequest;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.dto.response.ReactionResponse;
import thitkho.chatservice.dto.response.ReplyPreview;
import thitkho.chatservice.exception.MessageErrorCode;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.model.Message;
import thitkho.chatservice.model.RoomMember;
import thitkho.chatservice.repository.MessageReactionRepository;
import thitkho.chatservice.repository.MessageRepository;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.chatservice.repository.RoomRepository;
import thitkho.exception.AppException;
import thitkho.payload.CursorPage;
import thitkho.response.UserInfoChatResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final UserClient userClient;
    private final MessageRepository messageRepository;
    private final RoomRepository roomRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageReactionRepository messageReactionRepository;

    @Override
    public MessageResponse sendMessage(String userId, SendMessageRequest request) {
        RoomMember member = roomMemberRepository
                .findByRoomIdAndUserId(request.roomId(), userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        validateMessageContent(request);
        UserInfoChatResponse info = userClient.getUserById(userId);
        Message message = new Message();
        message.setRoomId(request.roomId());
        message.setSenderId(userId);
        message.setType(request.type());
        message.setContent(request.content());
        message.setFileName(request.fileName());
        message.setFileSize(request.fileSize());
        message.setMediaUrl(request.mediaUrl());
        message.setReplyToId(request.replyToId());
        messageRepository.save(message);

        // publish event to kafka
        //eventPublisher.publishEvent(new MessageSentEvent(message));

        ReplyPreview replyPreview = null;
        if (request.replyToId() != null) {
            Message replyToMsg = messageRepository.findById(request.replyToId())
                    .orElseThrow(() -> new AppException(MessageErrorCode.MESSAGE_REPLY_NOT_FOUND));
            UserInfoChatResponse replyToSender = userClient.getUserById(replyToMsg.getSenderId());
            replyPreview = ChatMapper.toReplyPreview(replyToMsg, replyToSender.displayName());
        }
        return ChatMapper.toMessageResponse(message, info.displayName(), info.avatar(), List.of(), replyPreview);
    }

    @Override
    public CursorPage<MessageResponse> getMessages(String userId, String roomId, String cursor, int limit) {
        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : null;
        List<Message> messages = messageRepository.findMessagesByRoomIdWithCursor(roomId, cursorTime, limit+1);
        boolean hasNext = messages.size() > limit;
        List<Message> pageMessages = hasNext ? messages.subList(0, limit) : messages;
        List<String> messageIds = pageMessages.stream().map(Message::getId).toList();
        Map<String, List<ReactionResponse>> reactionsMap = messageReactionRepository
                .getReactionsByMessageId(messageIds, userId)
                .stream()
                .collect(Collectors.groupingBy(ReactionResponse::messageId));

        List<String> senderIds = pageMessages.stream().map(Message::getSenderId).distinct().toList();
        Map<String, UserInfoChatResponse> userInfoMap = userClient.getUsersByIds(senderIds);

        List<String> replyToIds = pageMessages.stream()
                .map(Message::getReplyToId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, ReplyPreview> replyMap = messageRepository
                .findAllById(replyToIds)
                .stream()
                .collect(Collectors.toMap(Message::getId, replyMsg -> {
                    UserInfoChatResponse senderInfo = userInfoMap.get(replyMsg.getSenderId());
                    return ChatMapper.toReplyPreview(replyMsg, senderInfo.displayName());
                }));

        List<MessageResponse> responses = pageMessages.stream()
                .map(msg -> {
                    UserInfoChatResponse sender = userInfoMap.get(msg.getSenderId());
                    List<ReactionResponse> reactions = reactionsMap
                            .getOrDefault(msg.getId(), List.of());
                    ReplyPreview replyPreview = replyMap.get(msg.getReplyToId());
                    return ChatMapper.toMessageResponse(msg,
                            sender != null ? sender.displayName() : "Unknown",
                            sender != null ? sender.avatar() : null,
                            reactions,
                            replyPreview);
                })
                .toList();

        String nextCursor = hasNext
                ? pageMessages.getLast().getCreatedAt().toString()
                : null;

        return new CursorPage<>(responses, nextCursor, hasNext);
    }

    @Override
    public MessageResponse getMessage(String userId, String messageId) {
        return null;
    }

    @Override
    public MessageResponse editMessage(String userId, String messageId, String newContent) {
        return null;
    }

    @Override
    public void deleteMessage(String userId, String messageId) {

    }

    @Override
    public void updateLastRead(String userId, String roomId, UpdateLastReadRequest request) {

    }

    @Override
    public MessageResponse forwardMessage(String userId, String messageId, String targetRoomId) {
        return null;
    }

    @Override
    public void addReaction(String userId, String messageId, AddReactionRequest request) {

    }

    @Override
    public void removeReaction(String userId, String messageId, String emoji) {

    }

    @Override
    public boolean validateAccess(String userId, String roomId) {
        return false;
    }

    private void validateMessageContent(SendMessageRequest request){
        switch(request.type()){
            case TEXT -> {
                if(request.content() == null || request.content().isBlank()){
                    throw new AppException(MessageErrorCode.CONTENT_REQUIRED);
                }
            }
            case FILE -> {
                if(request.fileName() == null ){
                    throw new AppException(MessageErrorCode.FILE_NAME_REQUIRED);
                }
                if(request.mediaUrl() == null){
                    throw new AppException(MessageErrorCode.MEDIA_URL_REQUIRED);
                }
            }
            case SYSTEM -> {
                throw new AppException(MessageErrorCode.CANNOT_SEND_SYSTEM_MESSAGE);
            }
        }
    }
}
