package thitkho.payload.event.presence;

/**
 * @param type     USER_ONLINE | USER_OFFLINE
 * @param userId   user liên quan
 * @param lastSeen epoch ms — null khi ONLINE, có giá trị khi OFFLINE
 */
public record PresenceEvent(
        String type,
        String userId,
        Long lastSeen
) {}
