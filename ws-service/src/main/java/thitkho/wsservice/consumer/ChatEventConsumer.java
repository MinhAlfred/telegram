package thitkho.wsservice.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import thitkho.constant.KafkaTopics;
import thitkho.payload.event.ChatEvent;
import thitkho.payload.event.member.*;
import thitkho.payload.event.message.*;
import thitkho.payload.event.room.*;
import thitkho.wsservice.config.RedisRelayConfig;
import thitkho.wsservice.dto.RedisRelayMessage;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = KafkaTopics.ROOM_EVENTS, groupId = "ws-service")
    public void handleRoomEvents(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            String eventType = node.get("type").asText();

            // Message events
            try {
                switch (MessageEventType.valueOf(eventType)) {
                    case MESSAGE_SENT      -> handleMessageSent(node);
                    case MESSAGE_EDITED    -> handleMessageEdited(node);
                    case MESSAGE_DELETED   -> handleMessageDeleted(node);
                    case MESSAGE_FORWARDED -> handleMessageForwarded(node);
                    case REACTION_UPDATED  -> handleReactionUpdated(node);
                }
                return;
            } catch (IllegalArgumentException ignored) {}

            // Member events
            try {
                switch (MemberEventType.valueOf(eventType)) {
                    case MEMBER_ADDED        -> handleMemberAdded(node);
                    case MEMBER_REMOVED      -> handleMemberRemoved(node);
                    case MEMBER_LEFT         -> handleMemberLeft(node);
                    case MEMBER_ROLE_CHANGED -> handleMemberRoleChanged(node);
                }
                return;
            } catch (IllegalArgumentException ignored) {}

            // Room events on this topic
            if (RoomEventType.ROOM_READ.name().equals(eventType)) {
                handleRoomRead(node);
            } else {
                log.warn("Unknown event type in ROOM_EVENTS: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error handling ROOM_EVENTS", e);
        }
    }

    // ROOM_CREATED, ROOM_UPDATED, ROOM_DELETED
    @KafkaListener(topics = KafkaTopics.ROOM_METADATA, groupId = "ws-service")
    public void handleRoomMetadata(String raw) {
        try {
            JsonNode node = objectMapper.readTree(raw);
            String eventType = node.get("type").asText();

            switch (RoomEventType.valueOf(eventType)) {
                case ROOM_CREATED -> handleRoomCreated(node);
                case ROOM_UPDATED -> handleRoomUpdated(node);
                case ROOM_DELETED -> handleRoomDeleted(node);
                default -> log.warn("Unhandled room metadata event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error handling ROOM_METADATA", e);
        }
    }

    private void handleMessageSent(JsonNode node) throws JsonProcessingException {
        ChatEvent<MessageSentPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/messages");
    }

    private void handleMessageEdited(JsonNode node) throws JsonProcessingException {
        ChatEvent<MessageEditedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/messages");
    }

    private void handleMessageDeleted(JsonNode node) throws JsonProcessingException {
        ChatEvent<MessageDeletedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/messages");
    }

    private void handleMessageForwarded(JsonNode node) throws JsonProcessingException {
        ChatEvent<MessageForwardedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/messages");
    }

    private void handleReactionUpdated(JsonNode node) throws JsonProcessingException {
        ChatEvent<ReactionUpdatedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/messages");
    }

    private void handleMemberAdded(JsonNode node) throws JsonProcessingException {
        ChatEvent<MemberAddedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/members");
    }

    private void handleMemberRemoved(JsonNode node) throws JsonProcessingException {
        ChatEvent<MemberRemovedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/members");
    }

    private void handleMemberLeft(JsonNode node) throws JsonProcessingException {
        ChatEvent<MemberLeftPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/members");
    }

    private void handleMemberRoleChanged(JsonNode node) throws JsonProcessingException {
        ChatEvent<MemberRoleChangedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/members");
    }

    private void handleRoomRead(JsonNode node) throws JsonProcessingException {
        ChatEvent<RoomReadPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/rooms");
    }

    private void handleRoomCreated(JsonNode node) throws JsonProcessingException {
        ChatEvent<RoomCreatedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/rooms");
    }

    private void handleRoomUpdated(JsonNode node) throws JsonProcessingException {
        ChatEvent<RoomUpdatedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/rooms");
    }

    private void handleRoomDeleted(JsonNode node) throws JsonProcessingException {
        ChatEvent<RoomDeletedPayload> event = objectMapper.treeToValue(node, new TypeReference<>() {});
        pushToRoom(event, "/queue/rooms");
    }

    // Publish vào Redis channel — tất cả instance subscribe và forward vào local STOMP broker
    private <T> void pushToRoom(ChatEvent<T> event, String destination) {
        try {
            String dest = "/topic/room/" + event.roomId() + destination;
//            RedisRelayMessage relayMessage = new RedisRelayMessage(dest, event.payload());
            JsonNode payloadNode = objectMapper.valueToTree(event.payload());
            String json = objectMapper.writeValueAsString(Map.of("destination", dest, "payload", payloadNode));
            redisTemplate.convertAndSend(RedisRelayConfig.RELAY_CHANNEL, json);
        } catch (Exception e) {
            log.error("Failed to publish relay message to Redis dest={}", event.roomId(), e);
        }
    }
}