package thitkho.chatservice.service;

import org.springframework.web.multipart.MultipartFile;
import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.request.UpdateLastReadRequest;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.payload.CursorPage;

public interface MessageService {
    MessageResponse sendMessage(String userId, SendMessageRequest request);
    MessageResponse sendFileMessage(String userId, String roomId, MultipartFile file, String replyToId);
    CursorPage<MessageResponse> getMessages(String userId, String roomId, String cursor, int limit);
    void editMessage(String userId, String messageId, String newContent);
    void deleteMessage(String userId, String messageId);
    MessageResponse forwardMessage(String userId, String messageId, String targetRoomId);
    // Reaction
    void addReaction(String userId, String messageId, AddReactionRequest request);
    void removeReaction(String userId, String messageId);

}