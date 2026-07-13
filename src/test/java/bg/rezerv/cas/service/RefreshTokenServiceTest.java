package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.rezerv.cas.config.JwtProperties;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final Duration REFRESH_TTL = Duration.ofDays(14);

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private ValueOperations<String, String> valueOps;

    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        service = new RefreshTokenService(redis,
                new JwtProperties("secret", Duration.ofMinutes(15), REFRESH_TTL));
    }

    @Test
    void issue_записва_token_с_ttl_14_дни() {
        String token = service.issue(42L);

        assertThat(token).isNotBlank();
        verify(valueOps).set(eq("refresh:" + token), eq("42"), eq(REFRESH_TTL));
    }

    @Test
    void consume_валиден_token_връща_userId_и_го_трие_атомарно() {
        when(valueOps.getAndDelete("refresh:abc")).thenReturn("42");

        assertThat(service.consume("abc")).contains(42L);
    }

    @Test
    void consume_непознат_token_връща_празен_optional() {
        when(valueOps.getAndDelete(startsWith("refresh:"))).thenReturn(null);

        assertThat(service.consume("missing")).isEmpty();
    }
}
