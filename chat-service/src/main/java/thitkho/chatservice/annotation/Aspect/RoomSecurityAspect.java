package thitkho.chatservice.annotation.Aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import thitkho.chatservice.annotation.RequireRoomMember;
import thitkho.chatservice.exception.RoomErrorCode;
import thitkho.chatservice.repository.RoomMemberRepository;
import thitkho.chatservice.util.SecurityUtils;
import thitkho.exception.AppException;
import thitkho.exception.errorcode.CommonErrorCode;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class RoomSecurityAspect {
    private final RoomMemberRepository roomMemberRepository;

    @Before("@annotation(requireRoomMember)")
    public void checkMembership(JoinPoint joinPoint, RequireRoomMember requireRoomMember) {
        String currentUserId = SecurityUtils.getCurrentUserId();
        if (currentUserId == null) {
            throw new AppException(CommonErrorCode.UNAUTHORIZED);
        }

        String roomId = getRoomIdFromArgs(joinPoint, requireRoomMember.value());

        if (roomId == null) {
            log.error("Không tìm thấy tham số {} trong method {}", requireRoomMember.value(), joinPoint.getSignature().getName());
            throw new AppException(CommonErrorCode.INTERNAL_SERVER_ERROR);
        }

        boolean isMember = roomMemberRepository.existsByRoomIdAndUserId(roomId, currentUserId);

        if (!isMember) {
            log.warn("User {} cố gắng truy cập trái phép vào Room {}", currentUserId, roomId);
            throw new AppException(RoomErrorCode.NOT_A_MEMBER);
        }
    }

    private String getRoomIdFromArgs(JoinPoint joinPoint, String paramName) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = signature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if (parameterNames[i].equals(paramName)) {
                    return args[i].toString();
                }
            }
        }
        return null;
    }
}