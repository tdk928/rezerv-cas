package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.error.ApiException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompanyAssignmentServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private CompanyAssignmentService service;

    private final Role clientRole = Role.builder().id(4L).code("CLIENT").build();
    private final Role ownerRole = Role.builder().id(2L).code("BUSINESS_OWNER").build();

    @Test
    void assignCompanySetsCompanyIdAndAddsBusinessOwnerRole() {
        User user = User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Иванов")
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole)))
                .build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(roleRepository.findByCode("BUSINESS_OWNER")).thenReturn(Optional.of(ownerRole));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = service.assignCompany(42L, 100L);

        assertThat(response.companyId()).isEqualTo(100L);
        assertThat(response.roles()).containsExactlyInAnyOrder("CLIENT", "BUSINESS_OWNER");
        verify(userRepository).save(user);
        assertThat(user.getCompanyId()).isEqualTo(100L);
    }

    @Test
    void assignCompanyRejectsWhenUserAlreadyHasCompany() {
        User user = User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Иванов")
                .companyId(5L)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(clientRole, ownerRole))
                .build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.assignCompany(42L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("фирма");
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignCompanyRejectsMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCompany(99L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("не е намерен");
    }
}
