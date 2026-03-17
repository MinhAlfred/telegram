package thitkho.wsservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thitkho.wsservice.presence.PresenceService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final PresenceService presenceService;

    /**
     * Query trạng thái online của nhiều user cùng lúc.
     * GET /presence?userIds=id1,id2,id3
     *
     * Response:
     * {
     *   "id1": { "online": true,  "lastSeen": null },
     *   "id2": { "online": false, "lastSeen": 1718000000000 }
     * }
     */
    @GetMapping
    public Map<String, PresenceStatus> getPresence(@RequestParam List<String> userIds) {
        return userIds.stream().collect(Collectors.toMap(
                userId -> userId,
                userId -> {
                    boolean online = presenceService.isOnline(userId);
                    Long lastSeen  = online ? null : presenceService.getLastSeen(userId);
                    return new PresenceStatus(online, lastSeen);
                }
        ));
    }

    public record PresenceStatus(boolean online, Long lastSeen) {}
}
