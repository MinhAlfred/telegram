package thitkho.wsservice.presence;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Duy trì cache Redis: user:rooms:{userId} → Set<roomId>
 * Dùng để biết khi user online/offline cần broadcast tới những room nào.
 */
@Service
@RequiredArgsConstructor
public class UserRoomMappingService {

    static final String USER_ROOMS_PREFIX = "user:rooms:";

    private final StringRedisTemplate redisTemplate;

    public void addRoom(String userId, String roomId) {
        redisTemplate.opsForSet().add(USER_ROOMS_PREFIX + userId, roomId);
    }

    public void removeRoom(String userId, String roomId) {
        redisTemplate.opsForSet().remove(USER_ROOMS_PREFIX + userId, roomId);
    }

    public void removeRooms(String userId, List<String> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) return;
        redisTemplate.opsForSet().remove(USER_ROOMS_PREFIX + userId, roomIds.toArray(String[]::new));
    }

    public Set<String> getRooms(String userId) {
        Set<String> rooms = redisTemplate.opsForSet().members(USER_ROOMS_PREFIX + userId);
        return rooms != null ? rooms : Collections.emptySet();
    }
}
