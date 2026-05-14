package com.infosys.svpms.repository;

import com.infosys.svpms.entity.RoleAssignmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RoleAssignmentHistoryRepository extends JpaRepository<RoleAssignmentHistory, Long> {
    List<RoleAssignmentHistory> findByUserIdOrderByChangedAtDesc(Long userId);
    List<RoleAssignmentHistory> findAllByOrderByChangedAtDesc();
}
