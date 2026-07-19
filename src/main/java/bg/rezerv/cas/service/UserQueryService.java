package bg.rezerv.cas.service;

import bg.rezerv.cas.repository.UserRepository;
import bg.rezerv.cas.web.dto.UserSummaryResponse;
import bg.rezerv.cas.web.error.ApiException;
import java.util.Collection;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getById(Long userId) {
        return userRepository.findById(userId)
                .map(UserSummaryResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Потребителят не е намерен"));
    }

    @Transactional(readOnly = true)
    public List<UserSummaryResponse> findByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userRepository.findAllById(ids).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserSummaryResponse getByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EMAIL_REQUIRED", "Имейлът е задължителен");
        }
        String normalized = email.strip().toLowerCase();
        return userRepository.findByEmail(normalized)
                .map(UserSummaryResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND",
                        "Потребител с този email не е намерен"));
    }
}
