package thitkho.wsservice.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import thitkho.dto.OnlineStatus;
import thitkho.payload.event.presence.PresenceEvent;
import thitkho.wsservice.config.RedisRelayConfig;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishOnline(String userId) {
        publish(new PresenceEvent(OnlineStatus.ONLINE.name(), userId, null));
    }

    public void publishOffline(String userId, long lastSeen) {
        publish(new PresenceEvent(OnlineStatus.OFFLINE.name(), userId, lastSeen));
    }

    private void publish(PresenceEvent event) {
        try {
            String json = objectMapper.writeValueAsString(
                    Map.of("type", event.type(), "userId", event.userId(), "payload", event)
            );
            log.info("Publishing presence event: type={}, userId={}", event.type(), event.userId());
            redisTemplate.convertAndSend(RedisRelayConfig.RELAY_CHANNEL, json);
        } catch (Exception e) {
            log.error("Failed to publish presence event: userId={}", event.userId(), e);
        }
    }
}
