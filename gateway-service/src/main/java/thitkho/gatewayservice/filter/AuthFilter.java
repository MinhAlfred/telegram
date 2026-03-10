package thitkho.gatewayservice.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import thitkho.gatewayservice.config.PublicEndpointConfig;
import thitkho.gatewayservice.util.JwtUtil;

import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final PublicEndpointConfig publicEndpointConfig;

    @Value("${gateway.internal-secret}")
    private String internalSecret;

    private final JwtUtil jwtUtil;

    public AuthFilter(PublicEndpointConfig publicEndpointConfig, JwtUtil jwtUtil) {
        super(Config.class);
        this.publicEndpointConfig = publicEndpointConfig;
        this.jwtUtil = jwtUtil;
    }

    // 2. Class Config (có thể để trống nếu không cần tham số từ YAML)
    public static class Config {
    }

    // 3. Toàn bộ logic filter nằm ở đây
    @Override
    public GatewayFilter apply(Config config) {
        // Sử dụng OrderedGatewayFilter để giữ nguyên logic getOrder() = -1 của bạn
        return new OrderedGatewayFilter((exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();
            String method = exchange.getRequest().getMethod().name();

            log.debug("========== AuthFilter START ==========");
            log.debug("Incoming request: {} {}", method, path);
            log.debug("Original headers: {}", exchange.getRequest().getHeaders());

            // --- LOGIC CŨ CỦA BẠN ---
            ServerHttpRequest cleanedRequest = exchange.getRequest()
                    .mutate()
                    .headers(headers -> {
                        headers.remove("X-Internal-Secret");
                        headers.remove("X-User-Id");
                        headers.remove("X-User-Role");
                    })
                    .build();

            ServerWebExchange mutatedExchange = exchange.mutate().request(cleanedRequest).build();
            log.debug("Headers after cleaning: {}", mutatedExchange.getRequest().getHeaders());

            // 1. Bỏ qua public endpoints
            if (isPublicEndpoint(path)) {
                log.info("✅ Public endpoint detected: {} - Skipping JWT validation", path);
                ServerHttpRequest publicRequest = mutatedExchange.getRequest()
                        .mutate()
                        .header("X-Internal-Secret", internalSecret)
                        .build();
                log.debug("Added X-Internal-Secret for public endpoint");
                return chain.filter(mutatedExchange.mutate().request(publicRequest).build());
            }

            log.debug("Protected endpoint: {} - Validating JWT", path);

            // 2. Lấy token
            String token = extractToken(mutatedExchange.getRequest());
            if (token == null) {
                log.warn("❌ Missing token for protected endpoint: {}", path);
                return unauthorized(mutatedExchange, "Missing token");
            }

            log.debug("Token extracted: {}...", token.substring(0, Math.min(20, token.length())));

            // 3. Validate token
            if (!jwtUtil.isValid(token)) {
                log.warn("❌ Invalid token for path: {}", path);
                return unauthorized(mutatedExchange, "Invalid token");
            }

            log.debug("✅ Token is valid");

            // 4. Extract info
            String userId = jwtUtil.extractUserId(token);
            String role   = jwtUtil.extractRole(token);

            log.info("✅ Authenticated: userId={}, role={}, path={}", userId, role, path);

            ServerHttpRequest finalRequest = mutatedExchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .header("X-Internal-Secret", internalSecret)
                    .build();

            log.debug("Final headers forwarded to service: X-User-Id={}, X-User-Role={}", userId, role);
            log.debug("========== AuthFilter END ==========");

            return chain.filter(mutatedExchange.mutate().request(finalRequest).build());
        }, -1); // Order -1 ở đây
    }

    // --- CÁC HELPER METHODS GIỮ NGUYÊN ---

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            log.debug("Token extracted from Authorization header");
            return authHeader.substring(7);
        }
        String tokenParam = request.getQueryParams().getFirst("token");
        if (tokenParam != null) {
            log.debug("Token extracted from query parameter");
        }
        return tokenParam;
    }

    private boolean isPublicEndpoint(String path) {
        boolean isPublic = publicEndpointConfig.getEndpoints().stream()
                .anyMatch(endpoint -> path.equals(endpoint) || path.startsWith(endpoint));
        if (isPublic) {
            log.debug("Path {} matched public endpoint", path);
        }
        return isPublic;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.error("🔒 Unauthorized access: {} - Reason: {}",
                exchange.getRequest().getPath().value(), message);
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\": 401, \"message\": \"%s\"}".formatted(message);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}