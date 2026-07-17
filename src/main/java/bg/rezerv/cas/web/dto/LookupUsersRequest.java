package bg.rezerv.cas.web.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record LookupUsersRequest(@NotEmpty List<@NotNull Long> ids) {
}
