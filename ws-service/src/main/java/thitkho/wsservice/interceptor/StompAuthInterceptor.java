package thitkho.wsservice.interceptor;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import thitkho.wsservice.principal.StompPrincipal;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // Chỉ validate khi CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Gateway đã validate JWT và inject X-User-Id → HandshakeInterceptor lưu vào session attributes
            Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
            String userId = sessionAttrs != null ? (String) sessionAttrs.get("userId") : null;

            if (userId == null) {
                throw new MessageDeliveryException("Unauthorized");
            }

            accessor.setUser(new StompPrincipal(userId));
        }

        return message;
    }
}