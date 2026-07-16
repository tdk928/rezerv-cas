package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.dto.AuthResponse;
import bg.rezerv.cas.web.dto.LoginRequest;
import bg.rezerv.cas.web.dto.RefreshRequest;
import bg.rezerv.cas.web.dto.RegisterRequest;
import bg.rezerv.cas.web.error.ApiException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private CompanyAssignmentService companyAssignmentService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    private AuthService authService;

    private Role clientRole;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, roleRepository, passwordEncoder,
                jwtService, refreshTokenService, companyAssignmentService);
        clientRole = Role.builder().id(4L).code("CLIENT").build();
        lenient().when(companyAssignmentService.companyIdsFor(any())).thenReturn(List.of());
        lenient().when(companyAssignmentService.toResponse(any())).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return bg.rezerv.cas.web.dto.UserResponse.from(u, List.of());
        });
    }

    private User activeUser(String rawPassword) {
        return User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash(passwordEncoder.encode(rawPassword))
                .firstName("Иван")
                .lastName("Иванов")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(clientRole))
                .build();
    }

    @Test
    void register_създава_user_с_роля_CLIENT_и_връща_двата_token_а() {
        var request = new RegisterRequest("Ivan@Example.BG", "parola123", "Иван", "Иванов", null);
        when(userRepository.existsByEmail("ivan@example.bg")).thenReturn(false);
        when(roleRepository.findByCode("CLIENT")).thenReturn(Optional.of(clientRole));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.issueAccessToken(any())).thenReturn("access-jwt");
        when(refreshTokenService.issue(any())).thenReturn("refresh-uuid");
        when(jwtService.accessTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("ivan@example.bg");
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo("parola123");
        assertThat(passwordEncoder.matches("parola123", saved.getValue().getPasswordHash())).isTrue();
        assertThat(saved.getValue().getRoles()).containsExactly(clientRole);

        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
        assertThat(response.user().roles()).containsExactly("CLIENT");
    }

    @Test
    void register_дублиран_email_хвърля_409() {
        when(userRepository.existsByEmail("ivan@example.bg")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("ivan@example.bg", "parola123", "Иван", "Иванов", null)))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.code()).isEqualTo("EMAIL_ALREADY_EXISTS");
                });
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_с_валидни_credentials_връща_token_и() {
        when(userRepository.findByEmail("ivan@example.bg")).thenReturn(Optional.of(activeUser("parola123")));
        when(jwtService.issueAccessToken(any())).thenReturn("access-jwt");
        when(refreshTokenService.issue(42L)).thenReturn("refresh-uuid");
        when(jwtService.accessTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(new LoginRequest("ivan@example.bg", "parola123"));

        assertThat(response.accessToken()).isEqualTo("access-jwt");
        assertThat(response.user().id()).isEqualTo(42L);
    }

    @Test
    void login_с_грешна_парола_хвърля_401_без_да_издава_token() {
        when(userRepository.findByEmail("ivan@example.bg")).thenReturn(Optional.of(activeUser("parola123")));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ivan@example.bg", "greshna")))
                .isInstanceOfSatisfying(ApiException.class, ex -> {
                    assertThat(ex.status()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(ex.code()).isEqualTo("INVALID_CREDENTIALS");
                });
        verify(refreshTokenService, never()).issue(any());
    }

    @Test
    void login_блокиран_user_хвърля_403() {
        User blocked = activeUser("parola123");
        blocked.setStatus(UserStatus.BLOCKED);
        when(userRepository.findByEmail("ivan@example.bg")).thenReturn(Optional.of(blocked));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ivan@example.bg", "parola123")))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("USER_BLOCKED"));
    }

    @Test
    void refresh_валиден_token_издава_нова_двойка() {
        when(refreshTokenService.consume("old-refresh")).thenReturn(Optional.of(42L));
        when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser("parola123")));
        when(jwtService.issueAccessToken(any())).thenReturn("new-access");
        when(refreshTokenService.issue(42L)).thenReturn("new-refresh");
        when(jwtService.accessTtlSeconds()).thenReturn(900L);

        AuthResponse response = authService.refresh(new RefreshRequest("old-refresh"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        assertThat(response.refreshToken()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_невалиден_token_хвърля_401() {
        when(refreshTokenService.consume("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("bad")))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.code()).isEqualTo("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void me_връща_профила_на_потребителя() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(activeUser("parola123")));

        var response = authService.me(42L);

        assertThat(response.email()).isEqualTo("ivan@example.bg");
        assertThat(response.roles()).containsExactly("CLIENT");
    }

    @Test
    void me_несъществуващ_user_хвърля_404() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(99L))
                .isInstanceOfSatisfying(ApiException.class, ex ->
                        assertThat(ex.status()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
