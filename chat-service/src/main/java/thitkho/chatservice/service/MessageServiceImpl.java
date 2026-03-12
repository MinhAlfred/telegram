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
import thitkho.chatservice.exception.MessageErrorCode;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.model.Message;
import thitkho.chatservice.model.MessageReaction;
import thitkho.chatservice.model.Room;
import thitkho.chatservice.model.enums.MessageType;
import thitkho.chatservice.producer.ChatEventProducer;
import thitkho.chatservice.repository.RoomRepository;
import thitkho.constant.KafkaTopics;
import thitkho.dto.response.ReplyPreview;
import thitkho.payload.event.ChatEvent;
import thitkho.chatservice.repository.MessageReactionRepository;
import thitkho.chatservice.repository.MessageRepository;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.exception.AppException;
import thitkho.payload.CursorPage;
import thitkho.dto.response.UserInfoChatResponse;
import thitkho.payload.event.message.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageServiceImpl implements MessageService {
    private final UserClient userClient;
    private final MessageRepository messageRepository;
    private final RoomMemberRepository roomMemberRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final ChatEventProducer chatEventProducer;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public MessageResponse sendMessage(String userId, SendMessageRequest request) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> new AppException(RoomErrorCode.ROOM_NOT_FOUND));
        roomMemberRepository
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

        ReplyPreview replyPreview = null;
        if (request.replyToId() != null) {
            Message replyToMsg = messageRepository.findById(request.replyToId())
                    .orElseThrow(() -> new AppException(MessageErrorCode.MESSAGE_REPLY_NOT_FOUND));
            UserInfoChatResponse replyToSender = userClient.getUserById(replyToMsg.getSenderId());
            replyPreview = ChatMapper.toReplyPreview(replyToMsg, replyToSender.displayName());
        }

        room.setLastMessageSenderId(userId);
        room.setLastMessageContent(request.type() == MessageType.TEXT ? request.content() : ("[" + request.type().name() + "]"));
        room.setLastMessageAt(message.getCreatedAt());
        roomRepository.save(room);
        roomMemberRepository.incrementUnreadCount(request.roomId(),userId);
        publishMessageEvent(
                KafkaTopics.ROOM_EVENTS,
                request.roomId(),
                MessageEventType.MESSAGE_SENT,
                new MessageSentPayload(
                        message.getId(),
                        message.getRoomId(),
                        message.getSenderId(),
                        info.displayName(),
                        info.avatar(),
                        message.getType().name(),
                        message.getContent(),
                        message.getMediaUrl(),
                        message.getFileName(),
                        message.getFileSize(),
                        message.getReplyToId(),
                        replyPreview,
                        false,
                        message.getCreatedAt(),
                        message.getUpdatedAt()
                        ));
        return ChatMapper.toMessageResponse(message, info.displayName(), info.avatar(), List.of(), replyPreview);
    }

    @Override
    public CursorPage<MessageResponse> getMessages(String userId, String roomId, String cursor, int limit) {
        roomMemberRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        LocalDateTime cursorTime = cursor != null ? LocalDateTime.parse(cursor) : LocalDateTime.now();
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
    @Transactional
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
            publishMessageEvent(
                    KafkaTopics.ROOM_EVENTS,
                    message.getRoomId(),
                    MessageEventType.MESSAGE_EDITED,
                    new MessageEditedPayload(
                            message.getRoomId(),
                            message.getId(),
                            newContent,
                            true,
                            LocalDateTime.now()
                    )
            );
    }

    @Override
    @Transactional
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
        publishMessageEvent(
                KafkaTopics.ROOM_EVENTS,
                message.getRoomId(),
                MessageEventType.MESSAGE_DELETED,
                new MessageDeletedPayload(
                        message.getRoomId(),
                        message.getId(),
                        true,
                        LocalDateTime.now()
                )
        );
    }

    @Override
    @Transactional
    public MessageResponse forwardMessage(String userId, String messageId, String targetRoomId) {
        roomMemberRepository.findByRoomIdAndUserId(targetRoomId, userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        // 2. Lấy message gốc
        Message original = messageRepository.findById(messageId)
                .orElseThrow(() -> new AppException(MessageErrorCode.MESSAGE_NOT_FOUND));
        roomMemberRepository.findByRoomIdAndUserId(original.getRoomId(), userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
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
        roomMemberRepository.incrementUnreadCount(targetRoomId, userId);
        UserInfoChatResponse senderInfo = userClient.getUserById(userId);
        publishMessageEvent(
                KafkaTopics.ROOM_EVENTS,
                targetRoomId,
                MessageEventType.MESSAGE_FORWARDED,
                new MessageForwardedPayload(
                        forwarded.getId(),
                        targetRoomId,
                        forwarded.getSenderId(),
                        senderInfo.displayName(),
                        senderInfo.avatar(),
                        forwarded.getType().name(),
                        forwarded.getContent(),
                        forwarded.getMediaUrl(),
                        forwarded.getFileName(),
                        forwarded.getFileSize(),
                        true,
                        LocalDateTime.now()
                )
        );

        // 5. Build response

        return ChatMapper.toMessageResponse(forwarded,
                senderInfo.displayName(),
                senderInfo.avatar(),
                List.of(),
                null);
    }

    @Override
    @Transactional
    public void addReaction(String userId, String messageId, AddReactionRequest request) {
        Message message = findMessageById(messageId);
        roomMemberRepository.findByRoomIdAndUserId(message.getRoomId(), userId)
                .orElseThrow(() -> new AppException(RoomErrorCode.USER_NOT_IN_ROOM));
        String newEmoji = request.emoji();

        // 1. Tìm reaction cũ của User này cho Message này
        Optional<MessageReaction> existingOpt = messageReactionRepository
                .findByMessageIdAndUserId(messageId, userId);

        if (existingOpt.isPresent()) {
            MessageReaction existing = existingOpt.get();

            // Nếu nhấn lại đúng icon cũ -> Không làm gì (hoặc có thể coi là remove tùy UX)
            if (existing.getEmoji().equals(newEmoji)) {
                return; // Không thay đổi gì, nên không cần update DB hay publish event
            }

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
        chatEventProducer.publish(
                KafkaTopics.ROOM_EVENTS,
                message.getRoomId(),
                new ChatEvent<>(
                        MessageEventType.REACTION_UPDATED.name(),
                        message.getRoomId(),
                        new ReactionUpdatedPayload(
                                message.getRoomId(),
                                messageId,
                                message.getReactionSummary(),
                                userId,
                                newEmoji,
                                false
                        )
                )
        );
    }

    @Override
    @Transactional
    public void removeReaction(String userId, String messageId) {
        // 1. Tìm và xóa record reaction
        String emoji = null;
        Optional<MessageReaction> exist = messageReactionRepository.findByMessageIdAndUserId(messageId, userId);
        // 2. Nếu thực sự có xóa (tức là User đã thả icon đó trước đó)
        if (exist.isPresent()) {
            emoji= exist.get().getEmoji();
            messageReactionRepository.delete(exist.get());
            // Giảm count trong JSONB
            messageRepository.decrementReactionCount(messageId, emoji);
            Message message = findMessageById(messageId);
            chatEventProducer.publish(
                    KafkaTopics.ROOM_EVENTS,
                    message.getRoomId(),
                    new ChatEvent<>(
                            MessageEventType.REACTION_UPDATED.name(),
                            message.getRoomId(),
                            new ReactionUpdatedPayload(
                                    message.getRoomId(),
                                    messageId,
                                    message.getReactionSummary(),
                                    userId,
                                    emoji,
                                    true
                            )
                    )
            );
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
            case SYSTEM -> throw new AppException(MessageErrorCode.CANNOT_SEND_SYSTEM_MESSAGE);
        }
    }

    private void publishMessageEvent(
            String topic,
            String roomId,
            MessageEventType eventType,
            Object payload
    ) {
        chatEventProducer.publish(topic, roomId, new ChatEvent<>(eventType.name(), roomId, payload));
    }

}
