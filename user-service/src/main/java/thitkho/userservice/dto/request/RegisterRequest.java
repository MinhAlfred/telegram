package thitkho.userservice.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email(message = "Invalid email")
        String email,

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        @NotBlank
        String displayName
) {}