package thitkho.chatservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.service.MessageService;
import thitkho.payload.ApiResponse;
import thitkho.payload.CursorPage;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Tag(name = "Message Management", description = "APIs for managing chat messages")
public class MessageController {
    private final MessageService messageService;

    @PostMapping
    @Operation(description = "Send a new message to a room. Supports text, file, and reply messages.")
    public ApiResponse<MessageResponse> sendMessage(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody SendMessageRequest request) {
        return ApiResponse.success(messageService.sendMessage(userId, request));
    }

    @PostMapping(value = "/files", consumes = "multipart/form-data")
    @Operation(description = "Upload a file and send it as a message. File is uploaded to Cloudinary and stored as a FILE type message.")
    public ApiResponse<MessageResponse> sendFileMessage(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String roomId,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String replyToId) {
        return ApiResponse.success(messageService.sendFileMessage(userId, roomId, file, replyToId));
    }

    @GetMapping
    @Operation(description = "Get paginated messages from a room using cursor-based pagination. Returns messages sorted by creation time.")
    public ApiResponse<CursorPage<MessageResponse>> getMessages(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam String roomId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "50") int limit) {
        return ApiResponse.success(messageService.getMessages(userId, roomId, cursor, limit));
    }

    @PatchMapping("/{messageId}")
    @Operation(description = "Edit message content. Only text messages can be edited and only by the sender.")
    public ApiResponse<Void> editMessage(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String messageId,
            @RequestParam String content) {
        messageService.editMessage(userId, messageId, content);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}")
    @Operation(description = "Soft delete a message. Only message sender can delete their own messages.")
    public ApiResponse<Void> deleteMessage(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String messageId) {
        messageService.deleteMessage(userId, messageId);
        return ApiResponse.success(null);
    }

    @PostMapping("/{messageId}/forward")
    @Operation(description = "Forward a message to another room. Creates a copy of the message in target room.")
    public ApiResponse<MessageResponse> forwardMessage(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String messageId,
            @RequestParam String targetRoomId) {
        return ApiResponse.success(messageService.forwardMessage(userId, messageId, targetRoomId));
    }

    @PostMapping("/{messageId}/reactions")
    @Operation(description = "Add emoji reaction to a message. If user already reacted with different emoji, it will be replaced.")
    public ApiResponse<Void> addReaction(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String messageId,
            @RequestBody AddReactionRequest request) {
        messageService.addReaction(userId, messageId, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{messageId}/reactions")
    @Operation(description = "Remove emoji reaction from a message. User can only remove their own reactions.")
    public ApiResponse<Void> removeReaction(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String messageId) {
        messageService.removeReaction(userId, messageId);
        return ApiResponse.success(null);
    }
}
