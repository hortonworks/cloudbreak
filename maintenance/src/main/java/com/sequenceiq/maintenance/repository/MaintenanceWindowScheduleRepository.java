package com.sequenceiq.maintenance.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.maintenance.domain.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;

@Transactional(TxType.REQUIRED)
public interface MaintenanceWindowScheduleRepository extends JpaRepository<MaintenanceWindowSchedule, Long> {

    List<MaintenanceWindowSchedule> findByAccountIdAndArchivedFalse(String accountId);

    Optional<MaintenanceWindowSchedule> findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
            String accountId, MaintenanceScopeType scopeType, String scopeId);
}
