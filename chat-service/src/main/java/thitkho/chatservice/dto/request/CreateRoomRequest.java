package thitkho.chatservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import thitkho.chatservice.model.enums.RoomType;

import java.util.List;

public record CreateRoomRequest(
        @NotBlank String name,
        @NotEmpty
        List<String> memberIds
) {}