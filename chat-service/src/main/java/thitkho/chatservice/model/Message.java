package thitkho.chatservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import thitkho.chatservice.model.base.TimestampedBase;
import thitkho.chatservice.model.enums.MessageType;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Long> reactionSummary = new HashMap<>();

    // Reply
    private String replyToId;   // messageId được reply
    private boolean isDeleted = false;  // soft delete
    private boolean isEdited = false;   // đã bị chỉnh sửa sau khi gửi
    // Forward
    private boolean isForwarded = false; // đã bị chuyển tiếp
    private String forwardedAvatar;
    private String forwardedName;
}