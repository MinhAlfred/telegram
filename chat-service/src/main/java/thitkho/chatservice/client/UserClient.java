package thitkho.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import thitkho.dto.response.UserInfoChatResponse;


import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {
    @GetMapping("/api/feign/users/ids")
    Map<String, UserInfoChatResponse> getUsersByIds(List<String> userIds);

    @GetMapping(("/api/feign/user"))
    UserInfoChatResponse getUserById(String userId);
}
