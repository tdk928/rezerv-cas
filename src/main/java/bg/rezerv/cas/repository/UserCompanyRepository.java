package bg.rezerv.cas.repository;

import bg.rezerv.cas.domain.UserCompany;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserCompanyRepository extends JpaRepository<UserCompany, UserCompany.Pk> {

    boolean existsByUserIdAndCompanyId(Long userId, Long companyId);

    List<UserCompany> findByUserIdOrderByCreatedAtAsc(Long userId);
}
