package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.error.ApiException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserQueryServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserQueryService service;

    @Test
    void getByEmailNormalizesAndReturnsSummary() {
        User user = User.builder()
                .id(9L)
                .email("maria@example.bg")
                .passwordHash("h")
                .firstName("Мария")
                .lastName("Петрова")
                .status(UserStatus.ACTIVE)
                .build();
        when(userRepository.findByEmail("maria@example.bg")).thenReturn(Optional.of(user));

        var summary = service.getByEmail("  Maria@Example.bg ");

        assertThat(summary.id()).isEqualTo(9L);
        assertThat(summary.email()).isEqualTo("maria@example.bg");
    }

    @Test
    void getByEmailNotFound() {
        when(userRepository.findByEmail("missing@example.bg")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByEmail("missing@example.bg"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("не е намерен");
    }
}
