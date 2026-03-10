# Kafka Event Payloads Documentation

## Overview
Tất cả các event được publish vào Kafka đều sử dụng `ChatEvent<T>` wrapper:
```java
ChatEvent<T>(
    String type,        // Event type (enum.name())
    String roomId,      // Room ID để WS-Service biết push cho ai
    T payload           // Payload cụ thể theo từng event type
)
```

## Topics
- **ROOM_METADATA**: Room lifecycle events (ROOM_CREATED, ROOM_UPDATED, ROOM_DELETED, ROOM_READ)
- **ROOM_EVENTS**: Member & Message events (MEMBER_*, MESSAGE_*, REACTION_*)

## Event Types

### 1. Room Events (`thitkho.payload.event.room`)

#### RoomEventType Enum
```
ROOM_CREATED
ROOM_UPDATED
ROOM_DELETED
ROOM_READ
```

#### Payloads

**RoomCreatedPayload**
```java
{
    String id,
    String name,
    String avatar,
    String description,
    String type,              // DIRECT, GROUP
    String createdBy,
    String lastMessage,
    int memberCount,
    LocalDateTime lastMessageAt,
    LocalDateTime createdAt
}
```

**RoomUpdatedPayload**
```java
{
    String roomId,
    String name,
    String avatar,
    String description
}
```

**RoomDeletedPayload**
```java
{
    String roomId
}
```

**RoomReadPayload**
```java
{
    String roomId,
    String userId    // User đã đọc tin nhắn
}
```

---

### 2. Member Events (`thitkho.payload.event.member`)

#### MemberEventType Enum
```
MEMBER_ADDED
MEMBER_REMOVED
MEMBER_LEFT
MEMBER_ROLE_CHANGED
```

#### Payloads

**MemberAddedPayload**
```java
{
    String roomId,
    String actorUserId,        // Người thêm member
    List<String> memberIds,    // Danh sách user được thêm
    int memberCount,           // Số lượng member được thêm
    LocalDateTime occurredAt
}
```

**MemberRemovedPayload**
```java
{
    String roomId,
    String actorUserId,        // Người kick
    String targetUserId,       // User bị kick
    LocalDateTime occurredAt
}
```

**MemberLeftPayload**
```java
{
    String roomId,
    String userId,             // User tự rời phòng
    LocalDateTime occurredAt
}
```

**MemberRoleChangedPayload**
```java
{
    String roomId,
    String actorUserId,        // Người thay đổi role
    String targetUserId,       // User được thay đổi role
    String newRole,            // OWNER, ADMIN, MEMBER
    LocalDateTime occurredAt
}
```

---

### 3. Message Events (`thitkho.payload.event.message`)

#### MessageEventType Enum
```
MESSAGE_SENT
MESSAGE_EDITED
MESSAGE_DELETED
MESSAGE_FORWARDED
REACTION_UPDATED
```

#### Payloads

**MessageSentPayload**
```java
{
    String roomId,
    String messageId,
    String senderId,
    String messageType,        // TEXT, FILE, SYSTEM
    LocalDateTime occurredAt
}
```

**MessageEditedPayload**
```java
{
    String roomId,
    String messageId,
    String actorUserId,        // Người edit
    boolean edited,            // true
    LocalDateTime occurredAt
}
```

**MessageDeletedPayload**
```java
{
    String roomId,
    String messageId,
    String actorUserId,        // Người xóa
    boolean deleted,           // true
    LocalDateTime occurredAt
}
```

**MessageForwardedPayload**
```java
{
    String roomId,
    String messageId,
    String senderId,           // Người forward
    String sourceMessageId,    // Message gốc
    String messageType,        // TEXT, FILE
    boolean forwarded,         // true
    LocalDateTime occurredAt
}
```

**ReactionUpdatedPayload**
```java
{
    String roomId,
    String messageId,
    String userId,
    String emoji,
    String action,             // "REACTION_ADDED" hoặc "REACTION_REMOVED"
    LocalDateTime occurredAt
}
```

---

## Usage Example

### Producer Side (Chat-Service)
```java
// Member event
chatEventProducer.publish(
    KafkaTopics.ROOM_EVENTS,
    roomId,
    new ChatEvent<>(
        MemberEventType.MEMBER_ADDED.name(),
        roomId,
        new MemberAddedPayload(...)
    )
);

// Message event
chatEventProducer.publish(
    KafkaTopics.ROOM_EVENTS,
    roomId,
    new ChatEvent<>(
        MessageEventType.MESSAGE_SENT.name(),
        roomId,
        new MessageSentPayload(...)
    )
);

// Room event
chatEventProducer.publish(
    KafkaTopics.ROOM_METADATA,
    roomId,
    new ChatEvent<>(
        RoomEventType.ROOM_CREATED.name(),
        roomId,
        new RoomCreatedPayload(...)
    )
);
```

### Consumer Side (WS-Service)
```java
@KafkaListener(topics = KafkaTopics.ROOM_EVENTS)
public void handleRoomEvent(ChatEvent<?> event) {
    String eventType = event.type();
    String roomId = event.roomId();
    
    switch (eventType) {
        case "MESSAGE_SENT" -> {
            MessageSentPayload payload = (MessageSentPayload) event.payload();
            // Push to WebSocket
        }
        case "MEMBER_ADDED" -> {
            MemberAddedPayload payload = (MemberAddedPayload) event.payload();
            // Push to WebSocket
        }
        // ... other cases
    }
}
```

---

## Notes

1. **Type Safety**: Tất cả payload đều là record classes để đảm bảo immutable và type-safe
2. **Enum Usage**: Sử dụng enum để tránh typo và dễ refactor
3. **Timestamp**: Tất cả event đều có `occurredAt` để tracking thời gian
4. **Actor Tracking**: Events có `actorUserId` để biết ai thực hiện hành động
5. **Room Context**: Tất cả event đều có `roomId` để WS-Service biết push cho room nào

