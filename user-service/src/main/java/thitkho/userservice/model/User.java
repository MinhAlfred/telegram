package thitkho.userservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import thitkho.userservice.model.enums.AuthProvider;
import thitkho.userservice.model.enums.OnlineStatus;
import thitkho.userservice.model.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(unique = true, nullable = true)
    private String username;        // null nếu login Google

    @Column(nullable = false)
    private String email;

    @Column(nullable = true)
    private String password;        // null nếu login Google

    private String displayName;
    private String avatar;          // Cloudinary URL
    private String phoneNumber;
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private AuthProvider provider;  // LOCAL, GOOGLE

    private String providerId;      // Google sub ID

    @Enumerated(EnumType.STRING)
    private Role role;              // USER, ADMIN

    @Enumerated(EnumType.STRING)
    private OnlineStatus onlineStatus; // ONLINE, OFFLINE

    private LocalDateTime lastSeen;

    private boolean isActive;       // soft delete / ban
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @LastModifiedDate
    @Setter(AccessLevel.NONE)
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}