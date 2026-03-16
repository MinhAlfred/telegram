package thitkho.wsservice.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import thitkho.constant.KafkaTopics;
import thitkho.wsservice.config.RedisRelayConfig;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventConsumer {

    private final StringRedisTemplate redisTemplate;

    @KafkaListener(topics = KafkaTopics.ROOM_EVENTS, groupId = "ws-service")
    public void handleRoomEvents(String raw) {
        relay(raw);
    }

    @KafkaListener(topics = KafkaTopics.ROOM_METADATA, groupId = "ws-service")
    public void handleRoomMetadata(String raw) {
        relay(raw);
    }

    private void relay(String raw) {
        try {
            redisTemplate.convertAndSend(RedisRelayConfig.RELAY_CHANNEL, raw);
        } catch (Exception e) {
            log.error("Failed to relay event to Redis", e);
        }
    }
}
