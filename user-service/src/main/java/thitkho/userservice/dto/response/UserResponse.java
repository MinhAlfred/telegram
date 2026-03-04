package thitkho.userservice.dto.response;

import thitkho.userservice.model.enums.AuthProvider;
import thitkho.userservice.model.enums.OnlineStatus;
import thitkho.userservice.model.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record UserResponse(
        String id,
        String username,
        String email,
        String displayName,
        String avatar,
        String phoneNumber,
        LocalDate dateOfBirth,
        AuthProvider provider,
        Role role,
        OnlineStatus onlineStatus,
        LocalDateTime lastSeen,
        boolean isActive,
        LocalDateTime createdAt
) {}