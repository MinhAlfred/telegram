package thitkho.chatservice.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import thitkho.exception.ErrorCode;
@AllArgsConstructor
@Getter
public enum MessageErrorCode implements ErrorCode {
    CONTENT_REQUIRED("content_required", "Message content is required", 400),
    MEDIA_URL_REQUIRED("media_url_required", "Media URL is required for media messages", 400),
    FILE_NAME_REQUIRED("file_name_required", "File name is required for file messages", 400),
    CANNOT_SEND_SYSTEM_MESSAGE("cannot_send_system_message", "Cannot send system messages", 400),
    MESSAGE_NOT_FOUND("message_not_found", "Message not found", 404),
    MESSAGE_DELETED("message_deleted", "Message has been deleted", 410),
    CANNOT_EDIT_NON_TEXT("cannot_edit_non_text", "Only text messages can be edited", 400),
    NO_PERMISSION("no_permission", "User does not have permission to perform this action", 403),
    MESSAGE_REPLY_NOT_FOUND("message_reply_not_found", "Replied message not found", 404),
    USER_NOT_MESSAGE_OWNER("user_not_message_owner", "User is not the owner of the message", 403);

    private final String code;
    private final String message;
    private final int status;

}
