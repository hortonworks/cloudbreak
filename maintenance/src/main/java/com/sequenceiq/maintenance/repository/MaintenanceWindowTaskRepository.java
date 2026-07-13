package com.sequenceiq.maintenance.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.maintenance.domain.MaintenanceTaskStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowTask;

@Transactional(TxType.REQUIRED)
public interface MaintenanceWindowTaskRepository extends JpaRepository<MaintenanceWindowTask, Long> {

    List<MaintenanceWindowTask> findByStatusOrderByPriorityDescCreatedAtAsc(MaintenanceTaskStatus status);

    List<MaintenanceWindowTask> findByResourceCrn(String resourceCrn);

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

    List<MaintenanceWindowTask> findAllByAccountIdAndResourceCrnAndTaskTypeAndWorkItemIdAndStatus(
            String accountId, String resourceCrn, String taskType, String workItemId, MaintenanceTaskStatus status);
}
