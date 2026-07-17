package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.error.ApiException;
import java.util.List;
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
    void getById_връща_summary() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(User.builder()
                .id(5L)
                .email("owner@example.bg")
                .passwordHash("x")
                .firstName("Иван")
                .lastName("Иванов")
                .build()));

        var summary = service.getById(5L);

        assertThat(summary.id()).isEqualTo(5L);
        assertThat(summary.email()).isEqualTo("owner@example.bg");
        assertThat(summary.firstName()).isEqualTo("Иван");
        assertThat(summary.lastName()).isEqualTo("Иванов");
    }

    @Test
    void getById_хвърля_при_липса() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("не е намерен");
    }

    @Test
    void findByIds_празен_списък() {
        assertThat(service.findByIds(List.of())).isEmpty();
    }

    @Test
    void findByIds_връща_намираните() {
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(
                User.builder().id(1L).email("a@b.bg").passwordHash("x").firstName("A").lastName("B").build()));

        assertThat(service.findByIds(List.of(1L, 2L))).hasSize(1);
    }
}
