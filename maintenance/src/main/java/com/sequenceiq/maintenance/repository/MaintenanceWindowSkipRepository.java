package com.sequenceiq.maintenance.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;

@Transactional(TxType.REQUIRED)
public interface MaintenanceWindowSkipRepository extends JpaRepository<MaintenanceWindowSkip, Long> {

    List<MaintenanceWindowSkip> findByMaintenanceWindowScheduleId(Long maintenanceWindowScheduleId);

    Optional<MaintenanceWindowSkip> findByMaintenanceWindowScheduleIdAndWindowStart(Long maintenanceWindowScheduleId, Long windowStart);
}
