package thitkho.chatservice.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Stream;

public final class RoomUtils {

    private RoomUtils() {}

    public static String generateDirectHash(String userId1, String userId2) {
        String sorted = Stream.of(userId1, userId2).sorted().reduce((a, b) -> a + ":" + b).orElseThrow();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sorted.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
