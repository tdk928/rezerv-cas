package bg.rezerv.cas.web.dto;

import jakarta.validation.constraints.NotNull;

public record SwitchCompanyRequest(@NotNull Long companyId) {
}
