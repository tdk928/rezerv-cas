package bg.rezerv.cas.service;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserCompany;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserCompanyRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.dto.CreateStaffUserRequest;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.dto.UserSummaryResponse;
import bg.rezerv.cas.web.error.ApiException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Multi-company membership: user може да е owner на много фирми.
 * users.company_id = активната фирма (JWT); user_companies = всички membership-и.
 */
@Service
public class CompanyAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(CompanyAssignmentService.class);
    private static final String BUSINESS_OWNER_ROLE = "BUSINESS_OWNER";
    private static final String STAFF_ROLE = "STAFF";
    private static final String CLIENT_ROLE = "CLIENT";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserCompanyRepository userCompanyRepository;
    private final PasswordEncoder passwordEncoder;

    public CompanyAssignmentService(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    UserCompanyRepository userCompanyRepository,
                                    PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userCompanyRepository = userCompanyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse assignCompany(Long userId, Long companyId) {
        User user = requireActiveUser(userId);
        if (userCompanyRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            throw new ApiException(HttpStatus.CONFLICT, "COMPANY_ALREADY_ASSIGNED",
                    "Потребителят вече е свързан с тази фирма");
        }

        Role businessOwner = roleRepository.findByCode(BUSINESS_OWNER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Липсва seed роля " + BUSINESS_OWNER_ROLE));

        userCompanyRepository.save(UserCompany.builder()
                .userId(userId)
                .companyId(companyId)
                .build());
        user.getRoles().add(businessOwner);
        // Новата фирма става активна (onboarding UX + JWT companyId).
        user.setCompanyId(companyId);
        User saved = userRepository.save(user);
        log.info("Assigned companyId={} to user id={} (active), role={}", companyId, userId, BUSINESS_OWNER_ROLE);
        return toResponse(saved);
    }

    /**
     * Owner onboarding: нов акаунт с CLIENT+STAFF, membership към companyId.
     */
    @Transactional
    public UserSummaryResponse createStaffUser(CreateStaffUserRequest request) {
        String email = request.email().toLowerCase().strip();
        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS",
                    "Потребител с този email вече съществува");
        }
        Role clientRole = roleRepository.findByCode(CLIENT_ROLE)
                .orElseThrow(() -> new IllegalStateException("Липсва seed роля " + CLIENT_ROLE));
        Role staffRole = roleRepository.findByCode(STAFF_ROLE)
                .orElseThrow(() -> new IllegalStateException("Липсва seed роля " + STAFF_ROLE));

        User user = User.builder()
                .email(email)
                .phone(request.phone().strip())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName().strip())
                .lastName(request.lastName().strip())
                .companyId(request.companyId())
                .roles(new HashSet<>(Set.of(clientRole, staffRole)))
                .build();
        user = userRepository.save(user);
        userCompanyRepository.save(UserCompany.builder()
                .userId(user.getId())
                .companyId(request.companyId())
                .build());
        log.info("Created STAFF user id={} companyId={}", user.getId(), request.companyId());
        return UserSummaryResponse.from(user);
    }

    /**
     * Свързва user като STAFF към фирма: membership в user_companies + роля STAFF.
     * Не сменя активната company_id, освен ако е null (за да не пипа owner сесията).
     * Идемпотентно: повторно викане с вече свързан staff е OK.
     */
    @Transactional
    public UserResponse assignStaff(Long userId, Long companyId) {
        User user = requireActiveUser(userId);
        Role staffRole = roleRepository.findByCode(STAFF_ROLE)
                .orElseThrow(() -> new IllegalStateException("Липсва seed роля " + STAFF_ROLE));

        if (!userCompanyRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            userCompanyRepository.save(UserCompany.builder()
                    .userId(userId)
                    .companyId(companyId)
                    .build());
        }
        user.getRoles().add(staffRole);
        if (user.getCompanyId() == null) {
            user.setCompanyId(companyId);
        }
        User saved = userRepository.save(user);
        log.info("Assigned STAFF companyId={} to user id={}", companyId, userId);
        return toResponse(saved);
    }

    @Transactional
    public UserResponse switchActiveCompany(Long userId, Long companyId) {
        User user = requireActiveUser(userId);
        if (!userCompanyRepository.existsByUserIdAndCompanyId(userId, companyId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "NOT_COMPANY_MEMBER",
                    "Нямате достъп до тази фирма");
        }
        user.setCompanyId(companyId);
        User saved = userRepository.save(user);
        log.info("Switched active companyId={} for user id={}", companyId, userId);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<Long> companyIdsFor(Long userId) {
        return userCompanyRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(UserCompany::getCompanyId)
                .toList();
    }

    public UserResponse toResponse(User user) {
        return UserResponse.from(user, companyIdsFor(user.getId()));
    }

    private User requireActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Потребителят не е намерен"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED", "Потребителят е блокиран");
        }
        return user;
    }
}
