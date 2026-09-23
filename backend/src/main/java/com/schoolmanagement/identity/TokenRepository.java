package com.schoolmanagement.identity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.*;

public interface TokenRepository extends JpaRepository<AccountToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AccountToken> findByTokenHash(String tokenHash);
}
