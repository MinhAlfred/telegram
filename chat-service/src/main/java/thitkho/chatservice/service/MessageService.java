package thitkho.chatservice.service;

import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.request.UpdateLastReadRequest;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.payload.CursorPage;

public interface MessageService {
    MessageResponse sendMessage(String userId, SendMessageRequest request);
    CursorPage<MessageResponse> getMessages(String userId, String roomId, String cursor, int limit);
    MessageResponse getMessage(String userId, String messageId);
    MessageResponse editMessage(String userId, String messageId, String newContent);
    void deleteMessage(String userId, String messageId);
    void updateLastRead(String userId, String roomId, UpdateLastReadRequest request);
    // Thread
    MessageResponse forwardMessage(String userId, String messageId, String targetRoomId);
    // Reaction
    void addReaction(String userId, String messageId, AddReactionRequest request);
    void removeReaction(String userId, String messageId, String emoji);

    // Validate (dùng cho Chat-ws-service gọi qua Feign)
    boolean validateAccess(String userId, String roomId);
}