package thitkho.chatservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import thitkho.chatservice.model.base.CreatedAtBase;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_reactions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "emoji"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageReaction extends CreatedAtBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    private String messageId;
    private String userId;
    private String emoji;

}