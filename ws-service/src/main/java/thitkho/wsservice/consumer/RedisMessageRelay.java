package thitkho.wsservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
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

    private static final Map<String, String> EVENT_QUEUE_MAP;
    static {
        Map<String, String> m = new HashMap<>();
        for (MessageEventType t : MessageEventType.values()) m.put(t.name(), "/queue/messages");
        for (MemberEventType t  : MemberEventType.values())  m.put(t.name(), "/queue/members");
        for (RoomEventType t    : RoomEventType.values())    m.put(t.name(), "/queue/rooms");
        EVENT_QUEUE_MAP = Map.copyOf(m);
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode root    = objectMapper.readTree(message.getBody());
            String eventType = root.get("type").asText();
            String roomId    = root.get("roomId").asText();
            JsonNode payload = root.get("payload");

            String queue = EVENT_QUEUE_MAP.get(eventType);
            if (queue == null) {
                log.warn("No queue mapping for event type: {}", eventType);
                return;
            }

            String destination = "/topic/room/" + roomId + queue;
            log.info("Relaying event type={} to destination={}", eventType, destination);
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("Error relaying Redis message to WebSocket", e);
        }
    }
}
