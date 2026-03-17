package thitkho.wsservice.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionPresenceListener {

    private final PresenceService presenceService;
    private final PresenceEventPublisher presenceEventPublisher;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) {
            log.warn("SessionConnectedEvent missing principal");
            return;
        }
        String userId = user.getName();
        presenceService.markOnline(userId);
        presenceEventPublisher.publishOnline(userId);
    }
}
