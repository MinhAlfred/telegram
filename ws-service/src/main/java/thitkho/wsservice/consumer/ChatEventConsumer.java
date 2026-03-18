package thitkho.wsservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import thitkho.constant.KafkaTopics;
import thitkho.payload.event.member.MemberEventType;
import thitkho.payload.event.room.RoomEventType;
import thitkho.wsservice.config.RedisRelayConfig;
import thitkho.wsservice.presence.UserRoomMappingService;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRoomMappingService userRoomMappingService;

    @KafkaListener(topics = KafkaTopics.ROOM_EVENTS, groupId = "ws-service")
    public void handleRoomEvents(String raw) {
        updateMembership(raw);
        relay(raw);
    }

    @KafkaListener(topics = KafkaTopics.ROOM_METADATA, groupId = "ws-service")
    public void handleRoomMetadata(String raw) {
        updateMembership(raw);
        relay(raw);
    }

    private void updateMembership(String raw) {
        try {
            JsonNode root    = objectMapper.readTree(raw);
            String eventType = root.get("type").asText();
            JsonNode payload = root.get("payload");

            if (RoomEventType.ROOM_CREATED.name().equals(eventType)) {
                String roomId = payload.get("id").asText();
                payload.get("memberIds").forEach(id ->
                        userRoomMappingService.addRoom(id.asText(), roomId));

            } else if (MemberEventType.MEMBER_ADDED.name().equals(eventType)) {
                String roomId = root.get("roomId").asText();
                payload.get("memberIds").forEach(id ->
                        userRoomMappingService.addRoom(id.asText(), roomId));

            } else if (MemberEventType.MEMBER_REMOVED.name().equals(eventType)) {
                String roomId      = root.get("roomId").asText();
                String targetUserId = payload.get("targetUserId").asText();
                userRoomMappingService.removeRoom(targetUserId, roomId);

            } else if (MemberEventType.MEMBER_LEFT.name().equals(eventType)) {
                String roomId = root.get("roomId").asText();
                String userId = payload.get("userId").asText();
                userRoomMappingService.removeRoom(userId, roomId);

            } else if (RoomEventType.ROOM_DELETED.name().equals(eventType)) {
                String roomId = root.get("roomId").asText();
                List<String> memberIds = new ArrayList<>();
                payload.get("memberIds").forEach(id -> memberIds.add(id.asText()));
                memberIds.forEach(id -> userRoomMappingService.removeRoom(id, roomId));
            }

        } catch (Exception e) {
            log.error("Failed to update membership cache from event", e);
        }
    }

    private void relay(String raw) {
        try {
            redisTemplate.convertAndSend(RedisRelayConfig.RELAY_CHANNEL, raw);
        } catch (Exception e) {
            log.error("Failed to relay event to Redis", e);
        }
    }
}
