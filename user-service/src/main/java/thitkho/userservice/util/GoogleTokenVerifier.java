package thitkho.userservice.util;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import thitkho.exception.AppException;
import thitkho.userservice.dto.response.GoogleUserInfo;
import thitkho.userservice.exception.AuthErrorCode;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
public class GoogleTokenVerifier {
    private final GoogleIdTokenVerifier verifier;
    public GoogleTokenVerifier(@Value("${google.client-id}") String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                new GsonFactory()
        )
                .setAudience(Collections.singletonList(clientId))
                .build();
    }

    public GoogleUserInfo verify(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken == null)
                throw new AppException(AuthErrorCode.INVALID_GOOGLE_TOKEN);

            GoogleIdToken.Payload payload = googleIdToken.getPayload();

            return new GoogleUserInfo(
                    payload.getSubject(),           // sub → providerId
                    payload.getEmail(),
                    (String) payload.get("name"),
                    (String) payload.get("picture")
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new AppException(AuthErrorCode.INVALID_GOOGLE_TOKEN);
        }
    }
}