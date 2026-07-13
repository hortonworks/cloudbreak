package com.sequenceiq.maintenance.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;

@Transactional(TxType.REQUIRED)
public interface MaintenanceWindowRunRepository extends JpaRepository<MaintenanceWindowRun, Long> {

    Optional<MaintenanceWindowRun> findByMaintenanceWindowTaskIdAndWindowStart(Long maintenanceWindowTaskId, Long windowStart);

    List<MaintenanceWindowRun> findByMaintenanceWindowTaskId(Long maintenanceWindowTaskId);

    List<MaintenanceWindowRun> findByResourceCrn(String resourceCrn);

    List<MaintenanceWindowRun> findByAccountId(String accountId);

    List<MaintenanceWindowRun> findByStatus(MaintenanceRunStatus status);
}
