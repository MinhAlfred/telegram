package thitkho.chatservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import thitkho.chatservice.model.base.TimestampedBase;
import thitkho.chatservice.model.enums.MessageType;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Message extends TimestampedBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String roomId;
    private String senderId;

    @Enumerated(EnumType.STRING)
    private MessageType type;   // TEXT, IMAGE, FILE

    private String content;     // text content
    private String mediaUrl;    // Cloudinary URL cho image/file
    private String fileName;    // tên file gốc
    private Long fileSize;      // bytes

    // Thêm vào Entity Message
    @Column(columnDefinition = "jsonb") // Nếu dùng PostgreSQL
    private String reactionSummary; // Lưu: {"LIKE": 10, "HEART": 5}

    // Reply
    private String replyToId;   // messageId được reply

    // Thread
    private String threadId;    // messageId gốc của thread
    private int replyCount;     // số reply trong thread

    private boolean isDeleted;  // soft delete

}