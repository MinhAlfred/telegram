package thitkho.wsservice.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PresenceCleanupJob {

    @Value("${presence.stale-threshold-ms:30000}")
    private long staleThresholdMs;

    private final StringRedisTemplate redisTemplate;
    private final PresenceEventPublisher presenceEventPublisher;

    @Scheduled(fixedDelayString = "${presence.cleanup-interval-ms:20000}")
    public void cleanup() {
        long now = Instant.now().toEpochMilli();
        long staleScore = now - staleThresholdMs;

        Set<String> staleUsers = redisTemplate.opsForZSet()
                .rangeByScore(PresenceService.ONLINE_ZSET, 0, staleScore);

        if (staleUsers == null || staleUsers.isEmpty()) return;
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection stringRedisConn = (StringRedisConnection) connection;
            for (String userId : staleUsers) {
                stringRedisConn.set(PresenceService.LAST_SEEN_PREFIX + userId, String.valueOf(now));
                stringRedisConn.zRem(PresenceService.ONLINE_ZSET, userId);
            }
            return null; // Bắt buộc return null theo docs của Spring Data Redis
        });
        for (String userId : staleUsers) {
            presenceEventPublisher.publishOffline(userId, now);
        }

        log.info("Presence cleanup: removed {} stale user(s)", staleUsers.size());
    }
}
