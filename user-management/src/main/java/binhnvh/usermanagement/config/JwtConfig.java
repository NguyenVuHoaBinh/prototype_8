package binhnvh.usermanagement.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Setter
@Getter
@Configuration
@ConfigurationProperties(prefix = "application.security.jwt")
public class JwtConfig {

    private String secretKey;

    private long expiration;

    private String issuer;

}