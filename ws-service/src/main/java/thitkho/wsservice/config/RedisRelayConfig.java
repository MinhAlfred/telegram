package thitkho.wsservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import thitkho.wsservice.consumer.RedisMessageRelay;

@Configuration
public class RedisRelayConfig {

    // Tất cả instance subscribe cùng channel này để fanout STOMP message
    public static final String RELAY_CHANNEL = "ws:relay";

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            RedisMessageRelay relay) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(relay, new PatternTopic(RELAY_CHANNEL));
        return container;
    }
}
