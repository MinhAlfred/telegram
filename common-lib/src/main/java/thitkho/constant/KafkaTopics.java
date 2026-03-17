package thitkho.constant;

public final class KafkaTopics {
    private KafkaTopics() {}

    // Room Events: tin nhắn, thành viên, phòng, reaction - tất cả các thay đổi trong chat
    public static final String ROOM_EVENTS = "chat.room.events";

    // Room Metadata: thông tin cơ bản phòng - không bao giờ mất
    public static final String ROOM_METADATA = "chat.room.metadata";

    // Presence: user online/offline
    public static final String USER_PRESENCE = "user.presence";
}