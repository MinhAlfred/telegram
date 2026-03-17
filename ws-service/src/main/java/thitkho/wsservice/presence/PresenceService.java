package thitkho.wsservice.presence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class PresenceService {

    static final String ONLINE_ZSET      = "users:online";
    static final String LAST_SEEN_PREFIX = "user:last_seen:";

    private final StringRedisTemplate redisTemplate;

    /** Connect hoặc heartbeat — đều chỉ ZADD để refresh score */
    public void markOnline(String userId) {
        redisTemplate.opsForZSet().add(ONLINE_ZSET, userId, now());
    }

    public boolean isOnline(String userId) {
        return redisTemplate.opsForZSet().score(ONLINE_ZSET, userId) != null;
    }

    public Long getLastSeen(String userId) {
        String val = redisTemplate.opsForValue().get(LAST_SEEN_PREFIX + userId);
        return val != null ? Long.parseLong(val) : null;
    }

    private long now() {
        return Instant.now().toEpochMilli();
    }
}
