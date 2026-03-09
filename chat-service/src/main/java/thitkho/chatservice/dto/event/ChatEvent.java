package thitkho.chatservice.dto.event;

public record ChatEvent<T>(
        String type,        // MESSAGE_SENT, MESSAGE_EDITED...
        String roomId,      // WS-Service dùng để biết push cho ai
        T payload
) {}
