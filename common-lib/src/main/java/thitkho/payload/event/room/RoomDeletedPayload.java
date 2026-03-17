package thitkho.payload.event.room;

import java.util.List;

public record RoomDeletedPayload(
        String roomId,
        List<String> memberIds
) {}

