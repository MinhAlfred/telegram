package thitkho.wsservice.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import thitkho.dto.OnlineStatus;
import thitkho.payload.event.presence.PresenceEvent;
import thitkho.wsservice.config.RedisRelayConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRoomMappingService userRoomMappingService;

    public void publishOnline(String userId) {
        Set<String> rooms = userRoomMappingService.getRooms(userId);
        publish(new PresenceEvent(OnlineStatus.ONLINE.name(), userId, null), rooms);
    }

    public void publishOffline(String userId, long lastSeen) {
        Set<String> rooms = userRoomMappingService.getRooms(userId);
        publish(new PresenceEvent(OnlineStatus.OFFLINE.name(), userId, lastSeen), rooms);
    }

    private void publish(PresenceEvent event, Set<String> rooms) {
        try {
            Map<String, Object> msg = new HashMap<>();
            msg.put("type", event.type());
            msg.put("userId", event.userId());
            msg.put("roomIds", rooms);
            msg.put("payload", event);
            String json = objectMapper.writeValueAsString(msg);
            log.info("Publishing presence event: type={}, userId={}, rooms={}", event.type(), event.userId(), rooms);
            redisTemplate.convertAndSend(RedisRelayConfig.RELAY_CHANNEL, json);
        } catch (Exception e) {
            log.error("Failed to publish presence event: userId={}", event.userId(), e);
        }
    }
}
