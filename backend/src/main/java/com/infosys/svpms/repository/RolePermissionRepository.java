package com.infosys.svpms.repository;

import com.infosys.svpms.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByRoleId(Long roleId);
    Optional<RolePermission> findByRoleIdAndModule(Long roleId, String module);

    @Modifying
    @Query("DELETE FROM RolePermission rp WHERE rp.role.id = :roleId")
    void deleteByRoleId(@Param("roleId") Long roleId);

    /** AC #6: Load all permissions for a role name — used by PermissionEvaluator */
    @Query("SELECT rp FROM RolePermission rp JOIN rp.role r WHERE r.name = :roleName AND r.active = true")
    List<RolePermission> findByRoleName(@Param("roleName") String roleName);
}
