package thitkho.chatservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thitkho.chatservice.dto.response.ReactionResponse;
import thitkho.chatservice.model.MessageReaction;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageReactionRepository extends JpaRepository<MessageReaction, String> {
    List<MessageReaction> findByMessageId(String messageId);
    Optional<MessageReaction> findByMessageIdAndUserIdAndEmoji(
            String messageId, String userId, String emoji);
    void deleteByMessageIdAndUserIdAndEmoji(
            String messageId, String userId, String emoji);

    @Query("""
    SELECT new thitkho.chatservice.dto.response.ReactionResponse(
        mr.messageId,
        mr.emoji,
        COUNT(mr),
        Case 
           When SUM(CASE WHEN mr.userId = :currentUserId THEN 1 ELSE 0 END) > 0
              Then true 
              Else false
        End
    )
    FROM MessageReaction mr
    WHERE mr.messageId In :messageIds
    GROUP BY mr.emoji
    """)
    List<ReactionResponse> getReactionsByMessageId(
            @Param("messageIds") List<String> messageIds,
            @Param("currentUserId") String currentUserId
    );
}