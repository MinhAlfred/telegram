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
import thitkho.wsservice.util.JwtUtil;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        // Chỉ validate khi CONNECT
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = accessor.getFirstNativeHeader("Authorization");

            if (token == null || !token.startsWith("Bearer ")) {
                throw new MessageDeliveryException("Missing token");
            }

            token = token.substring(7);

            if (!jwtUtil.isValid(token)) {
                throw new MessageDeliveryException("Invalid token");
            }

            String userId = jwtUtil.extractUserId(token);

            // Set principal → dùng cho convertAndSendToUser
            accessor.setUser(new StompPrincipal(userId));
        }

        return message;
    }
}