package thitkho.wsservice.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import thitkho.wsservice.client.ChatClient;

import java.time.Instant;

import java.security.Principal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionPresenceListener {

    private final PresenceService presenceService;
    private final PresenceEventPublisher presenceEventPublisher;
    private final UserRoomMappingService userRoomMappingService;
    private final ChatClient chatClient;

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) {
            log.warn("SessionConnectedEvent missing principal");
            return;
        }
        log.info("User connected: {}", user.getName());
        String userId = user.getName();

        // Load rooms từ chat-service để populate cache trước khi publish online event
        loadRoomsIntoCache(userId);

        presenceService.markOnline(userId);
        presenceEventPublisher.publishOnline(userId);
    }

    @EventListener
    public void onDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) {
            log.warn("SessionDisconnectEvent missing principal");
            return;
        }
        String userId = user.getName();
        log.info("User disconnected: {}", userId);

        boolean wasOnline = presenceService.markOffline(userId);
        if (wasOnline) {
            long lastSeen = Instant.now().toEpochMilli();
            presenceEventPublisher.publishOffline(userId, lastSeen);
            userRoomMappingService.clearRooms(userId);
        }
    }

    private void loadRoomsIntoCache(String userId) {
        try {
            List<String> roomIds = chatClient.getRoomIdsByUserId(userId);
            roomIds.forEach(roomId -> userRoomMappingService.addRoom(userId, roomId));
            log.info("Loaded {} rooms into cache for userId={}", roomIds.size(), userId);
        } catch (Exception e) {
            log.error("Failed to load rooms for userId={}, presence broadcast may be incomplete", userId, e);
        }
    }
}
