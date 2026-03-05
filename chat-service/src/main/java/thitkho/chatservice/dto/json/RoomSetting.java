package thitkho.chatservice.dto.json;

import lombok.Data;

@Data
public class RoomSetting {
    private boolean allMembersAreAdmin = false;
    private boolean historyVisible = true;
    private int slowModeSeconds = 0;
    private boolean allowMemberInvites = true;
}