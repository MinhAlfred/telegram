package thitkho.exception.errorcode;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {
    INTERNAL_SERVER_ERROR("SYSTEM_500", "An unexpected internal server error occurred", 500),
    INVALID_INPUT("COMMON_400", "The request contains invalid input data", 400),
    UNAUTHORIZED("COMMON_401", "Authentication is required or has failed", 401),
    RESOURCE_NOT_FOUND("COMMON_404", "The requested resource could not be found", 404);
    private final String code;
    private final String message;
    private final int status;
}
