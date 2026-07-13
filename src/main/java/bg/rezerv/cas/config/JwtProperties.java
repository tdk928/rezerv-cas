package bg.rezerv.cas.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rezerv.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {
}
