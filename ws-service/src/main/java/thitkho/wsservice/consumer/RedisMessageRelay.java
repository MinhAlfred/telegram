package thitkho.wsservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import thitkho.dto.OnlineStatus;
import thitkho.payload.event.member.MemberEventType;
import thitkho.payload.event.message.MessageEventType;
import thitkho.payload.event.room.RoomEventType;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageRelay implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> ROOM_QUEUE_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        for (MessageEventType t : MessageEventType.values()) m.put(t.name(), "/queue/messages");
        for (MemberEventType t  : MemberEventType.values())  m.put(t.name(), "/queue/members");
        for (RoomEventType t    : RoomEventType.values())    m.put(t.name(), "/queue/rooms");
        ROOM_QUEUE_MAP = Map.copyOf(m);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode root    = objectMapper.readTree(message.getBody());
            String eventType = root.get("type").asText();
            JsonNode payload = root.get("payload");

            // JsonNode → Object (Map) để Spring serialize đúng JSON content
            Object payloadObj = objectMapper.treeToValue(payload, Object.class);

            // Presence events — route theo userId
            if (isPresenceEvent(eventType)) {
                String userId = root.get("userId").asText();
                messagingTemplate.convertAndSend("/topic/presence/" + userId, payloadObj);
                return;
            }

            // ROOM_CREATED / ROOM_DELETED — push tới từng member qua user topic
            if (RoomEventType.ROOM_CREATED.name().equals(eventType)
                    || RoomEventType.ROOM_DELETED.name().equals(eventType)) {
                String roomId = root.get("roomId").asText();
                Map<String, String> notification = Map.of("eventType", eventType, "roomId", roomId);
                JsonNode memberIds = payload.get("memberIds");
                if (memberIds != null && memberIds.isArray()) {
                    memberIds.forEach(id ->
                            messagingTemplate.convertAndSend("/topic/user/" + id.asText() + "/rooms", notification)
                    );
                }
                // ROOM_DELETED cũng push vào room topic để FE đang mở phòng biết mà đóng lại
                if (RoomEventType.ROOM_DELETED.name().equals(eventType)) {
                    messagingTemplate.convertAndSend("/topic/room/" + roomId + "/queue/rooms", notification);
                }
                return;
            }

            String roomId = root.get("roomId").asText();
            String queue  = ROOM_QUEUE_MAP.get(eventType);
            if (queue == null) {
                log.warn("No queue mapping for event type: {}", eventType);
                return;
            }

            // Push tới room topic cho tất cả đang mở phòng
            messagingTemplate.convertAndSend("/topic/room/" + roomId + queue, payloadObj);

            // MEMBER_ADDED: push tới user topic của member mới (chưa subscribe room)
            if (MemberEventType.MEMBER_ADDED.name().equals(eventType)) {
                Map<String, String> notification = Map.of("eventType", eventType, "roomId", roomId);
                JsonNode addedIds = payload.get("memberIds");
                if (addedIds != null && addedIds.isArray()) {
                    addedIds.forEach(id ->
                            messagingTemplate.convertAndSend("/topic/user/" + id.asText() + "/rooms", notification)
                    );
                }
            }

            // MEMBER_REMOVED: push tới user topic của người bị kick
            if (MemberEventType.MEMBER_REMOVED.name().equals(eventType)) {
                Map<String, String> notification = Map.of("eventType", eventType, "roomId", roomId);
                JsonNode targetUserId = payload.get("targetUserId");
                if (targetUserId != null) {
                    messagingTemplate.convertAndSend("/topic/user/" + targetUserId.asText() + "/rooms", notification);
                }
            }

            // MEMBER_LEFT: push tới user topic của người tự rời
            if (MemberEventType.MEMBER_LEFT.name().equals(eventType)) {
                Map<String, String> notification = Map.of("eventType", eventType, "roomId", roomId);
                JsonNode userId = payload.get("userId");
                if (userId != null) {
                    messagingTemplate.convertAndSend("/topic/user/" + userId.asText() + "/rooms", notification);
                }
            }

        } catch (Exception e) {
            log.error("Error relaying Redis message to WebSocket", e);
        }
    }

    private boolean isPresenceEvent(String type) {
        try {
            OnlineStatus.valueOf(type);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
