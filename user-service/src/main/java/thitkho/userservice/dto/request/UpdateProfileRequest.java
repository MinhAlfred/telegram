package thitkho.userservice.dto.request;

import java.time.LocalDate;

public record UpdateProfileRequest(
        String displayName,
        String phoneNumber,
        LocalDate dateOfBirth
) {}