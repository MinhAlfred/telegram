package thitkho.wsservice.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceCleanupJob {

    @Value("${presence.stale-threshold-ms:30000}")
    private long staleThresholdMs;

    private final StringRedisTemplate redisTemplate;
    private final PresenceEventPublisher presenceEventPublisher;
    private final UserRoomMappingService userRoomMappingService;

    /**
     * Lua script atomic: chỉ ZREM user nếu score của họ VẪN còn <= staleScore
     * tại thời điểm thực thi — tránh race condition với reconnect.
     * Trả về list userId đã bị remove thực sự.
     */
    private static final RedisScript<List> CLEANUP_SCRIPT = RedisScript.of("""
            local staleScore = tonumber(ARGV[1])
            local now        = ARGV[2]
            local prefix     = ARGV[3]
            local members    = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', staleScore)
            local removed    = {}
            for _, userId in ipairs(members) do
                local score = redis.call('ZSCORE', KEYS[1], userId)
                if score and tonumber(score) <= staleScore then
                    redis.call('ZREM', KEYS[1], userId)
                    redis.call('SET', prefix .. userId, now)
                    table.insert(removed, userId)
                end
            end
            return removed
            """, List.class);

    @Scheduled(fixedDelayString = "${presence.cleanup-interval-ms:20000}")
    public void cleanup() {
        long now = Instant.now().toEpochMilli();
        long staleScore = now - staleThresholdMs;

        @SuppressWarnings("unchecked")
        List<String> removedUsers = (List<String>) redisTemplate.execute(
                CLEANUP_SCRIPT,
                List.of(PresenceService.ONLINE_ZSET),
                String.valueOf(staleScore),
                String.valueOf(now),
                PresenceService.LAST_SEEN_PREFIX
        );

        if (removedUsers == null || removedUsers.isEmpty()) return;
        for (String userId : removedUsers) {
            presenceEventPublisher.publishOffline(userId, now);
        }
        for (String userId : removedUsers) {
            userRoomMappingService.clearRooms(userId);
        }

        log.info("Presence cleanup: removed {} stale user(s)", removedUsers.size());
    }
}
