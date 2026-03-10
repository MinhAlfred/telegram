package thitkho.userservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import thitkho.dto.response.UserInfoChatResponse;
import thitkho.userservice.dto.mapper.UserChatMapper;
import thitkho.userservice.exception.AuthErrorCode;
import thitkho.userservice.repository.UserRepository;
import thitkho.exception.AppException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/feign")
@RequiredArgsConstructor
@Validated
@Tag(name = "Feign Client", description = "Internal APIs for inter-service communication")
public class FeignController {
    private final UserRepository userRepository;

    @GetMapping("/user")
    @Operation(description = "Get user information by ID. Used by other services via Feign client.")
    public UserInfoChatResponse getUserById(@RequestParam String userId) {
        return userRepository.findById(userId)
                .map(UserChatMapper::toChatResponse)
                .orElseThrow(() -> new AppException(AuthErrorCode.USER_NOT_FOUND));
    }

    @GetMapping("/users/ids")
    @Operation(description = "Get multiple users information by IDs. Returns a map with userId as key.")
    public Map<String, UserInfoChatResponse> getUsersByIds(@RequestParam List<String> userIds) {
        return userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        UserChatMapper::toChatResponse
                ));
    }
}


