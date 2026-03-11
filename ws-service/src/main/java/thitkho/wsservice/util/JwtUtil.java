package thitkho.wsservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public Claims extractClaims(String token) {
        try {
            Claims claims = parseClaims(token).getPayload();
            log.debug("Successfully extracted claims from token. Subject: {}", claims.getSubject());
            return claims;
        } catch (JwtException e) {
            log.error("Failed to extract claims from token: {}", e.getMessage());
            throw e;
        }
    }

    private Jws<Claims> parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
        } catch (JwtException e) {
            log.error("Failed to parse JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isValid(String token) {
        try {
            extractClaims(token);
            log.debug("Token validation successful");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractUserId(String token) {
        String userId = extractClaims(token).getSubject();
        log.debug("Extracted userId: {}", userId);
        return userId;
    }

    public String extractRole(String token) {
        String role = extractClaims(token).get("role", String.class);
        log.debug("Extracted role: {}", role);
        return role;
    }
}