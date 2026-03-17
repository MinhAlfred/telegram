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

            // Room events — route theo roomId
            String queue = ROOM_QUEUE_MAP.get(eventType);
            if (queue == null) {
                log.warn("No queue mapping for event type: {}", eventType);
                return;
            }
            String roomId = root.get("roomId").asText();
            messagingTemplate.convertAndSend("/topic/room/" + roomId + queue, payloadObj);

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
