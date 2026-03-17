package thitkho.chatservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thitkho.chatservice.model.Room;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {
    List<Room> findByCreatedBy(String userId);

    Optional<Room> findByDirectHash(String directHash);

    @Query(value = """
    SELECT r.* FROM rooms r
    JOIN room_members m1 ON r.id = m1.room_id
    JOIN room_members m2 ON r.id = m2.room_id
    WHERE m1.user_id = :userId1 
      AND m2.user_id = :userId2 
      AND r.type = 'DIRECT' 
    LIMIT 1
    """, nativeQuery = true)
    Optional<Room> findPrivateRoom(String userId1, String userId2);

    @Query("SELECT r FROM Room r JOIN RoomMember rm ON r.id = rm.roomId " +
            "WHERE rm.userId = :userId AND r.isActive = true " +
            "ORDER BY r.lastMessageAt DESC") // Sắp xếp phòng có tin nhắn mới lên đầu
    Page<Room> findRoomsByUserId(@Param("userId") String userId, Pageable pageable);

    @Query("""
    SELECT r FROM Room r
    JOIN RoomMember rm ON rm.roomId = r.id
    WHERE rm.userId = :userId
    AND r.isActive = true
    AND COALESCE(r.lastMessageAt, r.createdAt) < :cursor
    ORDER BY COALESCE(r.lastMessageAt, r.createdAt) DESC
    LIMIT :limit
    """)
    List<Room> findRoomsByUserIdWithCursor(
            @Param("userId") String userId,
            @Param("cursor") LocalDateTime cursor,
            @Param("limit") int limit
    );

    @Modifying
    @Query("""
    UPDATE Room r SET
        r.lastMessageId = :messageId,
        r.lastMessageSenderId = :senderId,
        r.lastMessageContent = :content,
        r.lastMessageAt = :sentAt
    WHERE r.id = :roomId
""")
    void updateLastMessage(
            @Param("roomId") String roomId,
            @Param("messageId") String messageId,
            @Param("senderId") String senderId,
            @Param("content") String content,
            @Param("sentAt") LocalDateTime sentAt
    );

    @Modifying
    @Query("""
UPDATE Room r
SET r.memberCount = r.memberCount + :count
WHERE r.id = :roomId
""")
    void incrementMemberCount(String roomId, int count);

    @Modifying
    @Query("""
UPDATE Room r
SET r.memberCount = r.memberCount - 1
WHERE r.id = :roomId AND r.memberCount >= 2
""")
    void decrementMemberCount(String roomId);
}