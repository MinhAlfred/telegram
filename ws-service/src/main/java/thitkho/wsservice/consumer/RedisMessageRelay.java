package thitkho.wsservice.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisMessageRelay implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    // Nhận message từ Redis channel → forward vào local STOMP broker của instance này
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            JsonNode root = objectMapper.readTree(message.getBody());
            String destination = root.get("destination").asText();
            JsonNode payload = root.get("payload");
            messagingTemplate.convertAndSend(destination, payload);
        } catch (Exception e) {
            log.error("Error relaying Redis message to WebSocket", e);
        }
    }
}
