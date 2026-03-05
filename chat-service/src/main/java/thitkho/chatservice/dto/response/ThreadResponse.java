package thitkho.chatservice.dto.response;

import org.springframework.data.domain.Page;

public record ThreadResponse(
        MessageResponse parent,         // message gốc
        Page<MessageResponse> replies   // các reply trong thread
) {}