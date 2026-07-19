package bg.rezerv.cas.web;

import bg.rezerv.cas.service.CompanyAssignmentService;
import bg.rezerv.cas.service.UserQueryService;
import bg.rezerv.cas.web.dto.AssignCompanyRequest;
import bg.rezerv.cas.web.dto.CreateStaffUserRequest;
import bg.rezerv.cas.web.dto.LookupUsersRequest;
import bg.rezerv.cas.web.dto.UserResponse;
import bg.rezerv.cas.web.dto.UserSummaryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Service-to-service (REZERV.md §2.1) — gateway НЕ route-ва /internal/**.
 * Вика се от rezerv-business.
 */
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {

    private final CompanyAssignmentService companyAssignmentService;
    private final UserQueryService userQueryService;

    public InternalUserController(CompanyAssignmentService companyAssignmentService,
                                  UserQueryService userQueryService) {
        this.companyAssignmentService = companyAssignmentService;
        this.userQueryService = userQueryService;
    }

    @GetMapping("/{userId}")
    public UserSummaryResponse getUser(@PathVariable Long userId) {
        return userQueryService.getById(userId);
    }

    @PostMapping("/lookup")
    public List<UserSummaryResponse> lookupUsers(@Valid @RequestBody LookupUsersRequest request) {
        return userQueryService.findByIds(request.ids());
    }

    @GetMapping("/by-email")
    public UserSummaryResponse getByEmail(@RequestParam String email) {
        return userQueryService.getByEmail(email);
    }

    @PostMapping("/{userId}/assign-company")
    public UserResponse assignCompany(@PathVariable Long userId,
                                        @Valid @RequestBody AssignCompanyRequest request) {
        return companyAssignmentService.assignCompany(userId, request.companyId());
    }

    @PostMapping("/{userId}/assign-staff")
    public UserResponse assignStaff(@PathVariable Long userId,
                                    @Valid @RequestBody AssignCompanyRequest request) {
        return companyAssignmentService.assignStaff(userId, request.companyId());
    }

    @PostMapping("/create-staff")
    public UserSummaryResponse createStaff(@Valid @RequestBody CreateStaffUserRequest request) {
        return companyAssignmentService.createStaffUser(request);
    }
}
