package thitkho.chatservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import thitkho.chatservice.repository.RoomMemberRepository;

import java.util.List;

@RestController
@RequestMapping("/api/feign")
@RequiredArgsConstructor
public class FeignController {

    private final RoomMemberRepository roomMemberRepository;

    @GetMapping("/rooms/user-room-ids")
    public List<String> getRoomIdsByUserId(@RequestParam String userId) {
        return roomMemberRepository.findByUserId(userId)
                .stream()
                .map(m -> m.getRoomId())
                .toList();
    }
}