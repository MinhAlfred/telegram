package thitkho.userservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import thitkho.exception.ErrorCode;

@AllArgsConstructor
@Getter
public enum AuthErrorCode implements ErrorCode {
    INVALID_CREDENTIALS("AUTH_401", "Invalid credentials provided", 401),

    TOKEN_EXPIRED("AUTH_403", "The authentication token has expired", 403),

    ACCOUNT_LOCKED("AUTH_423", "The account is currently locked", 423),

    EMAIL_EXISTED("AUTH_409", "An account with this email already exists", 409),

    USERNAME_EXISTED("AUTH_409", "An account with this username already exists", 409),

    INSUFFICIENT_PERMISSIONS("AUTH_403", "You do not have permission to perform this action", 403),

    USER_NOT_FOUND("AUTH_404", "No account found with the provided credentials", 404),

    USER_BANNED("AUTH_403", "Your account has been banned. Please contact support for more information", 403),

    INVALID_GOOGLE_TOKEN("AUTH_401", "The Google token provided is invalid or expired", 401),

    WRONG_PASSWORD("AUTH_401", "The password you entered is incorrect", 401);
    private final String code;
    private final String message;
    private final int status;
}
