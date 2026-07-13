package bg.rezerv.cas.web.dto;

import bg.rezerv.cas.domain.Role;
import bg.rezerv.cas.domain.User;
import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String phone,
        String firstName,
        String lastName,
        Long companyId,
        String status,
        List<String> roles,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFirstName(),
                user.getLastName(),
                user.getCompanyId(),
                user.getStatus().name(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getCreatedAt());
    }
}
