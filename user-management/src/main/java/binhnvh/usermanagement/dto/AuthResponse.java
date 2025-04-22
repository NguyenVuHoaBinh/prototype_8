package binhnvh.usermanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    private String tokenType;

    private Long userId;

    private String username;

    private String email;

    @Builder.Default
    private List<String> roles = new ArrayList<>();

    private LocalDateTime expiresAt;
}