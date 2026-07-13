package bg.rezerv.cas.service;

import bg.rezerv.cas.config.JwtProperties;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Refresh token-и в Redis: {@code refresh:<token> -> userId}, TTL 14 дни.
 * Rotation: при всяко ползване старият се трие атомарно (GETDEL) и се издава нов.
 */
@Service
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final StringRedisTemplate redis;
    private final JwtProperties properties;

    public RefreshTokenService(StringRedisTemplate redis, JwtProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        redis.opsForValue().set(KEY_PREFIX + token, String.valueOf(userId), properties.refreshTtl());
        return token;
    }

    /** Атомарно взима и трие token-а. Празен Optional = невалиден/изтекъл/вече ползван. */
    public Optional<Long> consume(String token) {
        String userId = redis.opsForValue().getAndDelete(KEY_PREFIX + token);
        return Optional.ofNullable(userId).map(Long::valueOf);
    }
}
