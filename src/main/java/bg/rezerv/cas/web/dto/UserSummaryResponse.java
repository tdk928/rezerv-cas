package bg.rezerv.cas.web.dto;

import bg.rezerv.cas.domain.User;

/** Минимален user профил за service-to-service (без парола/роли). */
public record UserSummaryResponse(
        Long id,
        String email,
        String firstName,
        String lastName) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName());
    }
}
