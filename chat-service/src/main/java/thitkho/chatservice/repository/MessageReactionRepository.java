package thitkho.chatservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
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
}