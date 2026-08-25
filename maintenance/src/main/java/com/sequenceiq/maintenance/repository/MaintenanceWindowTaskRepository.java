package com.sequenceiq.maintenance.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.maintenance.domain.MaintenanceTaskKind;
import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;

@Transactional(TxType.REQUIRED)
public interface MaintenanceWindowTaskRepository extends JpaRepository<MaintenanceWindowTask, Long> {

    Optional<MaintenanceWindowTask> findByIdAndAccountId(Long id, String accountId);

    List<MaintenanceWindowTask> findByAccountIdAndIdIn(String accountId, Collection<Long> ids);

    List<MaintenanceWindowTask> findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus status);

    @Query("""
            SELECT t FROM MaintenanceWindowTask t
            WHERE t.accountId = :accountId
                AND (:resourceCrn IS NULL OR t.resourceCrn = :resourceCrn)
                AND (:environmentCrn IS NULL OR t.environmentCrn = :environmentCrn)
                AND (:taskType IS NULL OR t.taskType = :taskType)
                AND (:workItemId IS NULL OR t.workItemId = :workItemId)
                AND (:taskKind IS NULL OR t.taskKind = :taskKind)
                AND (:status IS NULL OR t.status = :status)
            """)
    List<MaintenanceWindowTask> findByAccountIdAndFilters(
            @Param("accountId") String accountId,
            @Param("resourceCrn") String resourceCrn,
            @Param("environmentCrn") String environmentCrn,
            @Param("taskType") String taskType,
            @Param("workItemId") String workItemId,
            @Param("taskKind") MaintenanceTaskKind taskKind,
            @Param("status") MaintenanceTaskStatus status);

    @Query("""
            SELECT t FROM MaintenanceWindowTask t
            WHERE t.accountId = :accountId
                AND t.resourceCrn = :resourceCrn
                AND t.taskType = :taskType
                AND t.workItemId = :workItemId
                AND t.status = com.sequenceiq.maintenance.domain.MaintenanceTaskStatus.ACTIVE
            """)
    Optional<MaintenanceWindowTask> findActiveByAccountIdAndResourceCrnAndTaskTypeAndWorkItemId(
            @Param("accountId") String accountId,
            @Param("resourceCrn") String resourceCrn,
            @Param("taskType") String taskType,
            @Param("workItemId") String workItemId);
}
