package com.schoolmanagement.identity;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByEmail(String email);

    @org.springframework.data.jpa.repository.Query(
        "select u from UserAccount u where lower(u.email) like lower(concat('%',:search,'%')) or lower(u.firstName) like lower(concat('%',:search,'%')) or lower(u.lastName) like lower(concat('%',:search,'%'))"
    )
    org.springframework.data.domain.Page<UserAccount> search(
        @org.springframework.data.repository.query.Param("search") String search,
        org.springframework.data.domain.Pageable page
    );
}
