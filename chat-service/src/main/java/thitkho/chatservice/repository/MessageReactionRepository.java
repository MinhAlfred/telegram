package thitkho.chatservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import thitkho.chatservice.dto.response.MessageReactionResponse;
import thitkho.chatservice.model.MessageReaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, String> {
    List<MessageReaction> findByMessageId(String messageId);
    Optional<MessageReaction> findByMessageIdAndUserId(
            String messageId, String userId);

    @Query("""
    SELECT new thitkho.chatservice.dto.response.MessageReactionResponse(
        mr.messageId,
        mr.emoji)
    FROM MessageReaction mr
    WHERE mr.messageId In :messageIds
    AND mr.userId = :currentUserId
    """)
    List<MessageReactionResponse> getUserReactionsByMessageId(
            @Param("messageIds") List<String> messageIds,
            @Param("currentUserId") String currentUserId
    );
    @Modifying
    @Transactional
    int deleteByMessageIdAndUserId(String messageId, String userId);

}