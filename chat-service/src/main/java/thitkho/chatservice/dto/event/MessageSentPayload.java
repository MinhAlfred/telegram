package thitkho.chatservice.dto.event;

import thitkho.chatservice.dto.response.MessageResponse;

public record MessageSentPayload(
        MessageResponse message
) {}
