package com.infosys.svpms.repository;

import com.infosys.svpms.entity.CustomRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomRoleRepository extends JpaRepository<CustomRole, Long> {
    Optional<CustomRole> findByName(String name);
    boolean existsByName(String name);
    List<CustomRole> findByActiveTrue();
    List<CustomRole> findBySystemTrue();
}
