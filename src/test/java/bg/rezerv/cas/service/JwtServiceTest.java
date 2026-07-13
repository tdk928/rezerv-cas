package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;

import bg.rezerv.cas.config.JwtProperties;
import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "dev-secret-change-me-0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(14)));
    }

    @Test
    void issueAccessToken_съдържа_всички_claims_по_договора() {
        User user = User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Иванов")
                .companyId(7L)
                .roles(Set.of(Role.builder().code("BUSINESS_OWNER").build()))
                .build();

        String token = jwtService.issueAccessToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.get("email", String.class)).isEqualTo("ivan@example.bg");
        assertThat(claims.get("roles", List.class)).containsExactly("BUSINESS_OWNER");
        assertThat(claims.get("companyId", Long.class)).isEqualTo(7L);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(Duration.ofMinutes(15).toMillis());
    }

    @Test
    void issueAccessToken_companyId_е_null_за_клиент() {
        User user = User.builder()
                .id(1L)
                .email("client@example.bg")
                .passwordHash("hash")
                .firstName("Клиент")
                .lastName("Клиентов")
                .roles(Set.of(Role.builder().code("CLIENT").build()))
                .build();

        String token = jwtService.issueAccessToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.get("companyId")).isNull();
    }

    @Test
    void accessTtlSeconds_връща_ttl_от_конфигурацията() {
        assertThat(jwtService.accessTtlSeconds()).isEqualTo(900);
    }
}
