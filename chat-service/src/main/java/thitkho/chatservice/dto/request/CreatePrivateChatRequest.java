package thitkho.chatservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreatePrivateChatRequest(
        @NotBlank String targetUserId
) {}