package thitkho.chatservice.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventProducer {
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(String topic, String key, Object payload) {
        kafkaTemplate.send(topic, key, payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish kafka event topic={} key={}", topic, key, ex);
                        return;
                    }
                    log.debug("Published kafka event topic={} key={}", topic, key);
                });
    }
}
