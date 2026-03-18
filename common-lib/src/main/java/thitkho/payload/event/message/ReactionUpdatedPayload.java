package thitkho.payload.event.message;

import java.time.LocalDateTime;
import java.util.Map;

public record ReactionUpdatedPayload(
        String roomId,
        String messageId,
        Map<String, Long> reactionSummary,  // {emoji -> count} trạng thái SAU hành động
        String reactorUserId,               // userId vừa react
        String reactorEmoji,                // emoji vừa được thêm/xóa
        String previousEmoji,               // emoji cũ của reactor (null nếu chưa react trước đó)
        boolean removed
) {}

