package thitkho.chatservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import thitkho.chatservice.dto.request.AddMemberRequest;
import thitkho.chatservice.dto.response.RoomMemberResponse;
import thitkho.chatservice.model.enums.MemberRole;
import thitkho.chatservice.service.MemberService;
import thitkho.payload.ApiResponse;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member Management", description = "APIs for managing room members")
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/{roomId}/add")
    @Operation(description = "Add new members to a room. Only room admins or owner can perform this action.")
    public ApiResponse<Void> addMembers(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId,
            @RequestBody AddMemberRequest request) {
        memberService.addMembers(userId, roomId, request);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{roomId}/{targetUserId}")
    @Operation(description = "Remove a member from room. Only room admins or owner can remove members.")
    public ApiResponse<Void> removeMember(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId,
            @PathVariable String targetUserId) {
        memberService.removeMember(userId, roomId, targetUserId);
        return ApiResponse.success(null);
    }

    @PostMapping("/leave/{roomId}")
    @Operation(description = "Leave a room. Any member can leave a room voluntarily.")
    public ApiResponse<Void> leaveRoom(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId) {
        memberService.leaveRoom(userId, roomId);
        return ApiResponse.success(null);
    }

    @GetMapping("/{roomId}")
    @Operation(description = "Get paginated list of members in a room. Returns member info with user details.")
    public ApiResponse<Page<RoomMemberResponse>> getMembers(
            @PathVariable String roomId,
            @PageableDefault(size = 20, sort = "joinedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(memberService.getMembers(roomId, pageable));
    }

    @PutMapping("{roomId}/{targetUserId}/role")
    @Operation(description = "Change member role in room. Only room owner can change roles.")
    public ApiResponse<Void> changeMemberRole(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String roomId,
            @PathVariable String targetUserId,
            @RequestParam MemberRole role) {
        memberService.changeMemberRole(userId, roomId, targetUserId, role);
        return ApiResponse.success(null);
    }
}



