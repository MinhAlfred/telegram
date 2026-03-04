package thitkho.userservice.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class GoogleUserInfo {
    private String subject;
    private String email;
    private String name;
    private String picture;
}
