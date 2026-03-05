package thitkho.chatservice.dto.request;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AddMemberRequest(
        @NotEmpty List<String> userIds
) {}