package thitkho.chatservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thitkho.chatservice.client.UserClient;
import thitkho.chatservice.dto.mapper.ChatMapper;
import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.response.MessageReactionResponse;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.dto.response.ReactionResponse;
import thitkho.chatservice.dto.response.ReplyPreview;
import thitkho.chatservice.exception.MessageErrorCode;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.model.Message;
import thitkho.chatservice.model.MessageReaction;
import thitkho.chatservice.model.RoomMember;
import thitkho.chatservice.model.enums.MessageType;
import thitkho.chatservice.repository.MessageReactionRepository;
import thitkho.chatservice.repository.MessageRepository;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.chatservice.repository.RoomRepository;
import thitkho.exception.AppException;
import thitkho.payload.CursorPage;
import thitkho.response.UserInfoChatResponse;

import java.time.LocalDateTime;
import java.util.*;
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
        List<String> replyToIds = pageMessages.stream()
                .map(Message::getReplyToId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Set<String> allSenderIds = new HashSet<>();
        pageMessages.forEach(m -> allSenderIds.add(m.getSenderId()));

        Map<String, Message> replyMsgsMap = messageRepository.findAllById(replyToIds)
                .stream().collect(Collectors.toMap(Message::getId, m -> m));

        replyMsgsMap.values().forEach(m -> allSenderIds.add(m.getSenderId()));

        Map<String, UserInfoChatResponse> userInfoMap = userClient.getUsersByIds(new ArrayList<>(allSenderIds));
        Map<String, String> userReactionsMap = messageReactionRepository.getUserReactionsByMessageId(messageIds, userId)
                .stream()
                .collect(Collectors.toMap(
                        MessageReactionResponse::messageId,
                        MessageReactionResponse::emoji,
                        (existing, replacement) -> existing // Guard phòng hờ dữ liệu lỗi có 2 reaction
                ));

        List<MessageResponse> responses = pageMessages.stream()
                .map(msg -> {
                    UserInfoChatResponse sender = userInfoMap.get(msg.getSenderId());
                    List<ReactionResponse> reactionRes = ChatMapper.mapReactions(
                            msg.getId(),
                            msg.getReactionSummary(), // Lấy từ trường JSONB của Entity Message
                            userReactionsMap.get(msg.getId())
                    );
                    Message reply = replyMsgsMap.get(msg.getReplyToId());
                    ReplyPreview replyPreview = null;
                    if (reply != null) {
                        UserInfoChatResponse replySender = userInfoMap.get(reply.getSenderId());
                        String senderName = (replySender != null) ? replySender.displayName() : "Unknown User";
                        replyPreview = ChatMapper.toReplyPreview(reply, senderName);
                    }
                    return ChatMapper.toMessageResponse(msg,
                            sender != null ? sender.displayName() : "Unknown",
                            sender != null ? sender.avatar() : null,
                            reactionRes,
                            replyPreview);
                }
                ).toList();

        String nextCursor = hasNext
                ? pageMessages.getLast().getCreatedAt().toString()
                : null;

        return new CursorPage<>(responses, nextCursor, hasNext);
    }

    @Override
    public void editMessage(String userId, String messageId, String newContent) {
            Message message = findMessageById(messageId);
            if(!message.getSenderId().equals(userId)){
                throw new AppException(MessageErrorCode.NO_PERMISSION);
            }
            if(message.isDeleted()){
                throw new AppException(MessageErrorCode.MESSAGE_DELETED);
            }
            if(message.getType() != MessageType.TEXT){
                throw new AppException(MessageErrorCode.CANNOT_EDIT_NON_TEXT);
            }
            if(newContent == null || newContent.isBlank()){
                throw new AppException(MessageErrorCode.CONTENT_REQUIRED);
            }
            message.setContent(newContent);
            message.setEdited(true);
            messageRepository.save(message);
            // publish event to kafka
            //eventPublisher.publishEvent(new MessageEditedEvent(message));
    }

    @Override
    public void deleteMessage(String userId, String messageId) {
        Message message = findMessageById(messageId);
        if(!message.getSenderId().equals(userId)){
            throw new AppException(MessageErrorCode.NO_PERMISSION);
        }
        if(message.isDeleted()){
            throw new AppException(MessageErrorCode.MESSAGE_DELETED);
        }
        message.setDeleted(true);
        messageRepository.save(message);
        // publish event to kafka
        //eventPublisher.publishEvent(new MessageDeletedEvent(message));
    }

    @Override
    public MessageResponse forwardMessage(String userId, String messageId, String targetRoomId) {
        roomMemberRepository.findByRoomIdAndUserId(targetRoomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));

        // 2. Lấy message gốc
        Message original = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(MessageErrorCode.MESSAGE_NOT_FOUND));

        if (original.isDeleted()) {
            throw new AppException(MessageErrorCode.MESSAGE_DELETED);
        }
        UserInfoChatResponse originalSender = userClient.getUserById(original.getSenderId());

        // 3. Tạo message mới — không copy replyToId (forward là tin mới hoàn toàn)
        Message forwarded = Message.builder()
                .roomId(targetRoomId)
                .senderId(userId)
                .type(original.getType())
                .content(original.getContent())
                .mediaUrl(original.getMediaUrl())
                .fileName(original.getFileName())
                .fileSize(original.getFileSize())
                .forwardedAvatar(originalSender.avatar())
                .forwardedName(originalSender.displayName())
                .isForwarded(true)
                .build();
        forwarded = messageRepository.save(forwarded);

        // 4. Update lastMessage async
//        eventPublisher.publishEvent(new MessageSentEvent(forwarded));

        // 5. Build response
        UserInfoChatResponse senderInfo = userClient.getUserById(userId);
        return ChatMapper.toMessageResponse(forwarded,
                senderInfo.displayName(),
                senderInfo.avatar(),
                List.of(),
                null);
    }

    @Override
    @Transactional
    public void addReaction(String userId, String messageId, AddReactionRequest request) {
        String newEmoji = request.emoji();

        // 1. Tìm reaction cũ của User này cho Message này
        Optional<MessageReaction> existingOpt = messageReactionRepository
                .findByMessageIdAndUserId(messageId, userId);

        if (existingOpt.isPresent()) {
            MessageReaction existing = existingOpt.get();

            // Nếu nhấn lại đúng icon cũ -> Không làm gì (hoặc có thể coi là remove tùy UX)
            if (existing.getEmoji().equals(newEmoji)) return;

            // TRƯỜNG HỢP ĐỔI ICON (ví dụ từ ❤️ sang 👍)
            // - Giảm count icon cũ trong JSONB
            messageRepository.decrementReactionCount(messageId, existing.getEmoji());
            // - Cập nhật record cũ sang icon mới
            existing.setEmoji(newEmoji);
            messageReactionRepository.save(existing);
        } else {
            // TRƯỜNG HỢP THẢ MỚI HOÀN TOÀN
            MessageReaction newReaction = MessageReaction.builder()
                    .messageId(messageId)
                    .emoji(newEmoji)
                    .userId(userId)
                    .build();
            messageReactionRepository.save(newReaction);
        }

        // 2. Tăng count icon mới trong JSONB
        messageRepository.incrementReactionCount(messageId, newEmoji);

        // 3. TODO: Gửi WebSocket thông báo cho mọi người trong Room
        // messagingTemplate.convertAndSend("/topic/room/" + roomId, reactionUpdateEvent);
    }

    @Override
    @Transactional
    public void removeReaction(String userId, String messageId, String emoji) {
        // 1. Tìm và xóa record reaction
        int deletedCount = messageReactionRepository.deleteByMessageIdAndUserIdAndEmoji(messageId, userId, emoji);

        // 2. Nếu thực sự có xóa (tức là User đã thả icon đó trước đó)
        if (deletedCount > 0) {
            // Giảm count trong JSONB
            messageRepository.decrementReactionCount(messageId, emoji);

            // 3. TODO: Gửi WebSocket thông báo remove
        }
    }

    //---------------------------
    //Helper method
    //---------------------------



    private Message findMessageById(String messageId) {
        return messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(MessageErrorCode.MESSAGE_NOT_FOUND));
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
