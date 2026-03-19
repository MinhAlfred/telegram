package thitkho.wsservice.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token == null) {
                log.warn("STOMP CONNECT rejected: missing token");
                throw new MessageDeliveryException("Unauthorized: missing token");
            }

            boolean valid;
            try {
                valid = jwtUtil.isValid(token);
            } catch (Exception e) {
                log.error("STOMP CONNECT: jwtUtil.isValid() threw unexpected exception", e);
                throw new MessageDeliveryException("Unauthorized: token validation error");
            }

            if (!valid) {
                log.warn("STOMP CONNECT rejected: invalid token");
                throw new MessageDeliveryException("Unauthorized: invalid token");
            }
            log.info("STOMP CONNECT token is valid, extracting userId");

            String userId;
            try {
                userId = jwtUtil.extractUserId(token);
            } catch (Exception e) {
                log.error("STOMP CONNECT: jwtUtil.extractUserId() threw unexpected exception", e);
                throw new MessageDeliveryException("Unauthorized: cannot extract userId");
            }

            log.info("STOMP CONNECT authenticated: userId={}", userId);
            accessor.setUser(new StompPrincipal(userId));
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        log.debug("STOMP native headers: {}", accessor.toNativeHeaderMap());
        log.debug("STOMP session attributes: {}", accessor.getSessionAttributes());

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Token extracted from Authorization header");
            return authHeader.substring(7);
        }

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            Object token = sessionAttrs.get("token");
            if (token instanceof String s && !s.isBlank()) {
                log.debug("Token extracted from session attribute");
                return s;
            }
        }

        log.warn("Token not found in headers={} or session={}", authHeader, accessor.getSessionAttributes());
        return null;
    }
}