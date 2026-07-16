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
        /** Активна фирма (JWT companyId / X-Company-Id). */
        Long companyId,
        /** Всички фирми, към които user-ът е свързан (membership). */
        List<Long> companyIds,
        String status,
        List<String> roles,
        Instant createdAt) {

    public static UserResponse from(User user, List<Long> companyIds) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getPhone(),
                user.getFirstName(),
                user.getLastName(),
                user.getCompanyId(),
                List.copyOf(companyIds),
                user.getStatus().name(),
                user.getRoles().stream().map(Role::getCode).sorted().toList(),
                user.getCreatedAt());
    }
}
