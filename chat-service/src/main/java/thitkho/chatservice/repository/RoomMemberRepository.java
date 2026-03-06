package thitkho.chatservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thitkho.chatservice.model.RoomMember;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomMemberRepository extends JpaRepository<RoomMember, String> {
    Page<RoomMember> findByUserId(String userId, Pageable pageable);
    Page<RoomMember> findByRoomId(String roomId, Pageable pageable);
    List<RoomMember> findByRoomId(String roomId);
    Optional<RoomMember> findByRoomIdAndUserId(String roomId, String userId);
    boolean existsByRoomIdAndUserId(String roomId, String userId);
    List<RoomMember> findAllByRoomIdIn(List<String> roomIds);
    void deleteByRoomIdAndUserId(String roomId, String userId);

    @Modifying
    @Query("""
    UPDATE RoomMember m SET m.unreadCount = m.unreadCount + 1
    WHERE m.roomId = :roomId AND m.userId != :senderId
""")
    void incrementUnreadCount(
            @Param("roomId") String roomId,
            @Param("senderId") String senderId
    );
}