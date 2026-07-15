package bg.rezerv.cas.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Service-to-service: rezerv-business вика след успешно POST /business/companies. */
public record AssignCompanyRequest(@NotNull @Positive Long companyId) {
}
