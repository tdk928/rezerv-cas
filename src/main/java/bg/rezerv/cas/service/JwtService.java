package bg.rezerv.cas.service;

import bg.rezerv.cas.config.JwtProperties;
import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(Role::getCode).sorted().toList();
        // companyId като string — gateway чете getStringClaim; числов claim → 401 → frontend logout
        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("roles", roles);
        if (user.getCompanyId() != null) {
            builder.claim("companyId", String.valueOf(user.getCompanyId()));
        }
        return builder
                .id(UUID.randomUUID().toString())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(now.plus(properties.accessTtl())))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public long accessTtlSeconds() {
        return properties.accessTtl().toSeconds();
    }
}
