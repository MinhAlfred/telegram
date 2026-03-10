package thitkho.userservice.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thitkho.userservice.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtils {

    @Value("${jwt.access-token.secret}")
    private String accessTokenSecret;

    @Value("${jwt.refresh-token.secret}")
    private String refreshTokenSecret;

    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    // ==================== GENERATE ====================

    public String generateAccessToken(User user) {
        return buildToken(user, accessTokenSecret, accessTokenExpiration);
    }

    public String generateRefreshToken(User user) {
        return buildToken(user, refreshTokenSecret, refreshTokenExpiration);
    }

    private String buildToken(User user, String secret, long expiration) {
        return Jwts.builder()
                .subject(user.getId())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .claim("username", user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(secret))
                .compact();
    }

    // ==================== VALIDATE ====================

    public boolean isValidAccessToken(String token) {
        return isValidToken(token, accessTokenSecret);
    }

    public boolean isValidRefreshToken(String token) {
        try {
            log.debug("Validating refresh token with refresh secret");
            boolean isValid = isValidToken(token, refreshTokenSecret);
            log.debug("Refresh token validation result: {}", isValid);
            return isValid;
        } catch (Exception e) {
            log.error("Error validating refresh token: {}", e.getMessage());
            return false;
        }
    }

    private boolean isValidToken(String token, String secret) {
        try {
            log.debug("Parsing token with provided secret");
            parseClaims(token, secret);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ==================== EXTRACT ====================

    public String extractUserId(String token) {
        return extractClaims(token, accessTokenSecret).getSubject();
    }

    public String extractUserIdFromRefresh(String token) {
        log.debug("Extracting userId from refresh token using refresh secret");
        String userId = extractClaims(token, refreshTokenSecret).getSubject();
        log.debug("Extracted userId from refresh token: {}", userId);
        return userId;
    }

    public String extractRole(String token) {
        return extractClaims(token, accessTokenSecret)
                .get("role", String.class);
    }

    public long getExpiration(String token) {
        Date expiration = extractClaims(token, accessTokenSecret).getExpiration();
        return expiration.getTime() - System.currentTimeMillis();
    }

    // ==================== HELPER ====================

    private Claims extractClaims(String token, String secret) {
        return parseClaims(token, secret).getPayload();
    }

    private Jws<Claims> parseClaims(String token, String secret) {
        return Jwts.parser()
                .verifyWith(getSigningKey(secret))
                .build()
                .parseSignedClaims(token);
    }

    private SecretKey getSigningKey(String secret) {
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (DecodingException e) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}