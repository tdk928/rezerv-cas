package bg.rezerv.cas.service;

import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.dto.AuthResponse;
import bg.rezerv.cas.web.dto.LoginRequest;
import bg.rezerv.cas.web.dto.RefreshRequest;
import bg.rezerv.cas.web.dto.RegisterRequest;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.error.ApiException;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String DEFAULT_ROLE = "CLIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().strip();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "Потребител с този email вече съществува");
        }
        var clientRole = roleRepository.findByCode(DEFAULT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Липсва seed роля " + DEFAULT_ROLE));

        User user = User.builder()
                .email(email)
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().strip())
                .lastName(request.lastName().strip())
                .roles(Set.of(clientRole))
                .build();
        user = userRepository.save(user);
        log.info("Registered user id={}", user.getId());
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().toLowerCase().strip())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                        "Грешен email или парола"));
        requireActive(user);
        log.info("Login user id={}", user.getId());
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Long userId = refreshTokenService.consume(request.refreshToken())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                        "Невалиден или изтекъл refresh token"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN",
                        "Невалиден или изтекъл refresh token"));
        requireActive(user);
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Потребителят не е намерен"));
        return UserResponse.from(user);
    }

    private void requireActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED", "Потребителят е блокиран");
        }
    }

    private AuthResponse tokens(User user) {
        String access = jwtService.issueAccessToken(user);
        String refresh = refreshTokenService.issue(user.getId());
        return new AuthResponse(access, refresh, jwtService.accessTtlSeconds(), UserResponse.from(user));
    }
}
