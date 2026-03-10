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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

        log.debug("[InternalSecretFilter] Internal secret validated, forwarding request: {} {}", method, path);
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