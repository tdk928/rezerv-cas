package bg.rezerv.cas.web;

import bg.rezerv.cas.service.CompanyAssignmentService;
import bg.rezerv.cas.web.dto.AssignCompanyRequest;
import bg.rezerv.cas.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service (REZERV.md §2.1) — gateway НЕ route-ва /internal/**.
 * Вика се от rezerv-business след POST /business/companies.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final CompanyAssignmentService companyAssignmentService;

    public InternalUserController(CompanyAssignmentService companyAssignmentService) {
        this.companyAssignmentService = companyAssignmentService;
    }

    @PostMapping("/{userId}/assign-company")
    public UserResponse assignCompany(@PathVariable Long userId,
                                        @Valid @RequestBody AssignCompanyRequest request) {
        return companyAssignmentService.assignCompany(userId, request.companyId());
    }
}
