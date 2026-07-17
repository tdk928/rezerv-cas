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
}
