package thitkho.chatservice.model;

import jakarta.persistence.*;
import lombok.*;
import thitkho.chatservice.model.enums.MemberRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "room_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"room_id", "user_id"}))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomMember {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String roomId;
    private String userId;

    @Enumerated(EnumType.STRING)
    private MemberRole role;    // OWNER, ADMIN, MEMBER

    private LocalDateTime joinedAt;
    private LocalDateTime lastReadAt;  // để tính unread count
    private int unreadCount = 0;   // để tính unread count
}