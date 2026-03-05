package thitkho.chatservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import thitkho.exception.errorcode.ErrorCode;
@AllArgsConstructor
@Getter
public enum RoomErrorCode implements ErrorCode {
    INVALID_PRIVATE_ROOM("ROOM_001", "Private room must have exactly 2 members", 400),
    ROOM_NOT_FOUND("ROOM_002", "Room not found", 404),
    NOT_A_MEMBER("ROOM_003", "You are not a member of this room", 403),
    USER_ALREADY_IN_ROOM("ROOM_004", "User is already a member of this room", 400),
    USER_NOT_IN_ROOM("ROOM_005", "User is not a member of this room", 404),
    INVALID_ROOM_TYPE("ROOM_006", "Invalid room type", 400);

    private final String code;
    private final String message;
    private final int status;
}
