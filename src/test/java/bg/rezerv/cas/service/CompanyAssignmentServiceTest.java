package bg.rezerv.cas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserCompany;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserCompanyRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.error.ApiException;
import java.util.HashSet;
import java.util.List;
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

    @Mock
    private UserCompanyRepository userCompanyRepository;

    @InjectMocks
    private CompanyAssignmentService service;

    private final Role clientRole = Role.builder().id(4L).code("CLIENT").build();
    private final Role ownerRole = Role.builder().id(2L).code("BUSINESS_OWNER").build();

    @Test
    void assignCompanyAddsMembershipSetsActiveAndAddsBusinessOwnerRole() {
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
        when(userCompanyRepository.existsByUserIdAndCompanyId(42L, 100L)).thenReturn(false);
        when(roleRepository.findByCode("BUSINESS_OWNER")).thenReturn(Optional.of(ownerRole));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findByUserIdOrderByCreatedAtAsc(42L))
                .thenReturn(List.of(UserCompany.builder().userId(42L).companyId(100L).build()));

        var response = service.assignCompany(42L, 100L);

        assertThat(response.companyId()).isEqualTo(100L);
        assertThat(response.companyIds()).containsExactly(100L);
        assertThat(response.roles()).containsExactlyInAnyOrder("CLIENT", "BUSINESS_OWNER");
        verify(userCompanyRepository).save(any(UserCompany.class));
        verify(userRepository).save(user);
    }

    @Test
    void assignCompanyAllowsSecondCompanyAndSetsItActive() {
        User user = User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Иванов")
                .companyId(5L)
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole, ownerRole)))
                .build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userCompanyRepository.existsByUserIdAndCompanyId(42L, 100L)).thenReturn(false);
        when(roleRepository.findByCode("BUSINESS_OWNER")).thenReturn(Optional.of(ownerRole));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findByUserIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(
                UserCompany.builder().userId(42L).companyId(5L).build(),
                UserCompany.builder().userId(42L).companyId(100L).build()));

        var response = service.assignCompany(42L, 100L);

        assertThat(response.companyId()).isEqualTo(100L);
        assertThat(response.companyIds()).containsExactly(5L, 100L);
        verify(userCompanyRepository).save(any(UserCompany.class));
    }

    @Test
    void assignCompanyRejectsDuplicateMembership() {
        User user = User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Иванов")
                .companyId(100L)
                .status(UserStatus.ACTIVE)
                .roles(Set.of(clientRole, ownerRole))
                .build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userCompanyRepository.existsByUserIdAndCompanyId(42L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> service.assignCompany(42L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("тази фирма");
        verify(userRepository, never()).save(any());
    }

    @Test
    void switchActiveCompanyUpdatesCompanyId() {
        User user = User.builder()
                .id(42L)
                .email("ivan@example.bg")
                .passwordHash("hash")
                .firstName("Иван")
                .lastName("Иванов")
                .companyId(5L)
                .status(UserStatus.ACTIVE)
                .roles(new HashSet<>(Set.of(clientRole, ownerRole)))
                .build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(userCompanyRepository.existsByUserIdAndCompanyId(42L, 100L)).thenReturn(true);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userCompanyRepository.findByUserIdOrderByCreatedAtAsc(42L)).thenReturn(List.of(
                UserCompany.builder().userId(42L).companyId(5L).build(),
                UserCompany.builder().userId(42L).companyId(100L).build()));

        var response = service.switchActiveCompany(42L, 100L);

        assertThat(response.companyId()).isEqualTo(100L);
        assertThat(user.getCompanyId()).isEqualTo(100L);
    }

    @Test
    void switchActiveCompanyRejectsNonMember() {
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
        when(userCompanyRepository.existsByUserIdAndCompanyId(42L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.switchActiveCompany(42L, 99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("достъп");
    }

    @Test
    void assignCompanyRejectsMissingUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignCompany(99L, 100L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("не е намерен");
    }
}
