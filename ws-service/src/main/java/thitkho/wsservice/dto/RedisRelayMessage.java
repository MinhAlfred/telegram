package thitkho.wsservice.dto;

public record RedisRelayMessage(
        String destination,
        Object payload
) {}