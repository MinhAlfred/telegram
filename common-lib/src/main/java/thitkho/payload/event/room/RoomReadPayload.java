package thitkho.payload.event.room;

public record RoomReadPayload(
        String roomId,
        String userId    // để WS biết push cho đúng user
) {}