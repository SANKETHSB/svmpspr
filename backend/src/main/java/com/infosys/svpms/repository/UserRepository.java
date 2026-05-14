package com.infosys.svpms.repository;

import com.infosys.svpms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<User> findByLockedTrue();
    List<User> findByRole(User.Role role);

    @Query("SELECT u FROM User u WHERE (:search IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%',:search,'%')) OR LOWER(u.email) LIKE LOWER(CONCAT('%',:search,'%'))) AND (:role IS NULL OR u.role = :role)")
    Page<User> searchUsers(@Param("search") String search, @Param("role") User.Role role, Pageable pageable);

    /** AC #5: Count active users assigned a specific role name (enum or custom) */
    @Query("SELECT COUNT(u) FROM User u WHERE CAST(u.role AS string) = :roleName AND u.active = true")
    long countByRoleName(@Param("roleName") String roleName);

    /** AC #5: Count users with a custom role name */
    @Query("SELECT COUNT(u) FROM User u WHERE u.customRoleName = :roleName AND u.active = true")
    long countByCustomRoleName(@Param("roleName") String roleName);
}
