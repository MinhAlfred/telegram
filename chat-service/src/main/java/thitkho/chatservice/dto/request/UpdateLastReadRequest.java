package thitkho.chatservice.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record UpdateLastReadRequest(
        @NotNull LocalDateTime lastReadAt
) {}