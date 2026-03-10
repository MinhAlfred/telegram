package thitkho.chatservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import thitkho.dto.response.UserInfoChatResponse;


import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service", url = "${user-service.url}")
public interface UserClient {
    @GetMapping("/api/feign/users/ids")
    Map<String, UserInfoChatResponse> getUsersByIds(@RequestParam List<String> userIds);

    @GetMapping("/api/feign/user")
    UserInfoChatResponse getUserById(@RequestParam String userId);
}
