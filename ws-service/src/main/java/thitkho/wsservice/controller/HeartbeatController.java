package thitkho.wsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import thitkho.wsservice.presence.PresenceService;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class HeartbeatController {

    private final PresenceService presenceService;

    @MessageMapping("/heartbeat")
    public void heartbeat(Principal principal) {
        if (principal != null) {
            presenceService.markOnline(principal.getName());
        }
    }
}
