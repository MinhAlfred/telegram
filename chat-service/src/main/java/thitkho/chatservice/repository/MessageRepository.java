package thitkho.chatservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thitkho.chatservice.model.Message;

import java.time.LocalDateTime;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    // Load chat history
    Page<Message> findByRoomIdAndIsDeletedFalse(String roomId, Pageable pageable);

    // Load thread
    Page<Message> findByThreadIdAndIsDeletedFalse(String threadId, Pageable pageable);

    // Unread count
    @Query("""
            SELECT COUNT(m) FROM Message m
            WHERE m.roomId = :roomId
            AND m.createdAt > :lastReadAt
            AND m.isDeleted = false
            AND m.senderId != :userId
            """)
    long countUnread(@Param("roomId") String roomId,
                     @Param("userId") String userId,
                     @Param("lastReadAt") LocalDateTime lastReadAt);
}