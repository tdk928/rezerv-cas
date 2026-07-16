package bg.rezerv.cas.service;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserCompany;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserCompanyRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.error.ApiException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserCompanyRepository userCompanyRepository;

    public CompanyAssignmentService(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    UserCompanyRepository userCompanyRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userCompanyRepository = userCompanyRepository;
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
