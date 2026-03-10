package thitkho.chatservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import thitkho.chatservice.dto.json.RoomSetting;
import thitkho.chatservice.model.base.TimestampedBase;
import thitkho.chatservice.model.enums.RoomType;

import java.time.LocalDateTime;

@Entity
@Table(name = "rooms")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Room extends TimestampedBase {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;
    private String avatar;
    private String description;

    @Enumerated(EnumType.STRING)
    private RoomType type;      // DIRECT, GROUP

    private boolean isPublic = false; // chỉ áp dụng cho GROUP, DIRECT luôn là false

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private RoomSetting setting = new RoomSetting();
    @Column(unique = true)
    private String inviteCode;
    private int memberCount = 0; // để tối ưu hiển thị số lượng thành viên ở list phòng

    private String createdBy;   // userId
    private String lastMessageId; // để tối ưu truy vấn tin nhắn mới nhất
    private String lastMessageSenderId; // để hiển thị nhanh ở list phòng mà không cần join Message
    private String lastMessageContent; // lưu tạm để hiển thị nhanh ở list phòng
    private LocalDateTime lastMessageAt; // để sắp xếp phòng theo tin nhắn mới nhất
    private boolean isActive = true;

}