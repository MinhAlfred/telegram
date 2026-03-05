package thitkho.chatservice.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AddReactionRequest(
        @NotBlank String emoji
) {}