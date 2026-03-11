package thitkho.userservice.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalSecretFilter extends OncePerRequestFilter {

    @Value("${internal.secret}")
    private String internalSecret;

    private static final List<String> PUBLIC_PATHS = List.of(
            "/v3/api-docs",
            "/swagger-ui",
            "/users/auth/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        log.debug("[InternalSecretFilter] Incoming request {} {}", method, path);

        if (isPublicPath(path)) {
            log.debug("[InternalSecretFilter] Public path matched, bypassing internal secret check: {}", path);
            filterChain.doFilter(request, response);
            return;
        }

        String secret = request.getHeader("X-Internal-Secret");
        boolean hasSecret = secret != null && !secret.isBlank();
        log.debug("[InternalSecretFilter] Protected path={}, has X-Internal-Secret header={}", path, hasSecret);

        if (!internalSecret.equals(secret)) {
            log.warn("[InternalSecretFilter] Access denied for path={} (missing/invalid internal secret)", path);
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status": 403, "message": "Access denied"}
                    """);
            return;
        }
        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        String formattedRole = role != null ? (role.startsWith("ROLE_") ? role : "ROLE_" + role) : "ROLE_USER";
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(formattedRole));

        // 2. Tạo đối tượng Authentication
        // Quan trọng: Đối số đầu tiên chính là "Principal".
        // Khi bạn để userId ở đây, @AuthenticationPrincipal sẽ lấy được nó.
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, authorities);

        // (Tùy chọn) Lưu thêm chi tiết request nếu cần
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // 3. Set vào SecurityContext
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[InternalSecretFilter] Authenticated user {} with role {}", userId, formattedRole);
        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(path::startsWith);
        if (isPublic) {
            log.trace("[InternalSecretFilter] Path {} matched PUBLIC_PATHS", path);
        }
        return isPublic;
    }
}