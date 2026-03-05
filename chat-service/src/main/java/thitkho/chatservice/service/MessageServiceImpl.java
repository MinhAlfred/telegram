package thitkho.chatservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import thitkho.chatservice.dto.request.AddReactionRequest;
import thitkho.chatservice.dto.request.SendMessageRequest;
import thitkho.chatservice.dto.request.UpdateLastReadRequest;
import thitkho.chatservice.dto.response.MessageResponse;
import thitkho.chatservice.dto.response.ThreadResponse;

public class MessageServiceImpl implements MessageService {
    @Override
    public MessageResponse sendMessage(String userId, SendMessageRequest request) {
        return null;
    }

    @Override
    public Page<MessageResponse> getMessages(String userId, String roomId, Pageable pageable) {
        return null;
    }

    @Override
    public MessageResponse getMessage(String userId, String messageId) {
        return null;
    }

    @Override
    public void deleteMessage(String userId, String messageId) {

    }

    @Override
    public void updateLastRead(String userId, String roomId, UpdateLastReadRequest request) {

    }

    @Override
    public long getUnreadCount(String userId, String roomId) {
        return 0;
    }

    @Override
    public ThreadResponse getThread(String userId, String threadId, Pageable pageable) {
        return null;
    }

    @Override
    public void addReaction(String userId, String messageId, AddReactionRequest request) {

    }

    @Override
    public void removeReaction(String userId, String messageId, String emoji) {

    }

    @Override
    public boolean validateAccess(String userId, String roomId) {
        return false;
    }
}
