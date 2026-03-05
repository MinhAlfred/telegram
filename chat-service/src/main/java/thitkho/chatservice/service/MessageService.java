package thitkho.chatservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.request.UpdateLastReadRequest;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.dto.response.ThreadResponse;

public interface MessageService {
    MessageResponse sendMessage(String userId, SendMessageRequest request);
    Page<MessageResponse> getMessages(String userId, String roomId, Pageable pageable);
    MessageResponse getMessage(String userId, String messageId);
    void deleteMessage(String userId, String messageId);
    void updateLastRead(String userId, String roomId, UpdateLastReadRequest request);
    long getUnreadCount(String userId, String roomId);
    // Thread
    ThreadResponse getThread(String userId, String threadId, Pageable pageable);

    // Reaction
    void addReaction(String userId, String messageId, AddReactionRequest request);
    void removeReaction(String userId, String messageId, String emoji);

    // Validate (dùng cho Chat-ws-service gọi qua Feign)
    boolean validateAccess(String userId, String roomId);

}