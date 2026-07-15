package bg.rezerv.cas.service;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import bg.rezerv.cas.domain.UserStatus;
import bg.rezerv.cas.repository.RoleRepository;
import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.error.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Свързва user с фирма след B2B onboarding в rezerv-business:
 * задава company_id и добавя роля BUSINESS_OWNER (запазва CLIENT).
 */
@Service
public class CompanyAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(CompanyAssignmentService.class);
    private static final String BUSINESS_OWNER_ROLE = "BUSINESS_OWNER";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public CompanyAssignmentService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
    public UserResponse assignCompany(Long userId, Long companyId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Потребителят не е намерен"));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ApiException(HttpStatus.FORBIDDEN, "USER_BLOCKED", "Потребителят е блокиран");
        }
        if (user.getCompanyId() != null) {
            throw new ApiException(HttpStatus.CONFLICT, "COMPANY_ALREADY_ASSIGNED",
                    "Потребителят вече е свързан с фирма");
        }

        Role businessOwner = roleRepository.findByCode(BUSINESS_OWNER_ROLE)
                .orElseThrow(() -> new IllegalStateException("Липсва seed роля " + BUSINESS_OWNER_ROLE));

        user.setCompanyId(companyId);
        user.getRoles().add(businessOwner);
        User saved = userRepository.save(user);
        log.info("Assigned companyId={} to user id={}, role={}", companyId, userId, BUSINESS_OWNER_ROLE);
        return UserResponse.from(saved);
    }
}
