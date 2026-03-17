package thitkho.wsservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import thitkho.wsservice.presence.PresenceService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
@Slf4j
public class HeartbeatController {

    private final PresenceService presenceService;

    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        if (principal != null) {
            log.info("Received heartbeat request for {}", principal.getName());
            presenceService.markOnline(principal.getName());
        }
    }
}
