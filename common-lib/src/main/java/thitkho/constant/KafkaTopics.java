package thitkho.constant;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String MESSAGE_SENT    = "chat.message.sent";
    public static final String MESSAGE_EDITED  = "chat.message.edited";
    public static final String MESSAGE_DELETED = "chat.message.deleted";
    public static final String REACTION_UPDATED = "chat.reaction.updated";
    public static final String ROOM_UPDATED    = "chat.room.updated";
    public static final String MEMBER_UPDATED  = "chat.member.updated";
}