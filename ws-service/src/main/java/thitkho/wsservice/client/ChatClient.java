package thitkho.wsservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "chat-service", url = "${chat-service.url}", configuration = FeignInternalInterceptor.class)
public interface ChatClient {
    @GetMapping("/api/feign/rooms/user-room-ids")
    List<String> getRoomIdsByUserId (@RequestParam String userId);

}
