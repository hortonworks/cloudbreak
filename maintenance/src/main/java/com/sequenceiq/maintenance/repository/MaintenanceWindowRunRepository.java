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

    /**
     * Overlapping run for a prerequisite task relative to a dependent {@code [windowStart, windowEnd)} occurrence.
     * Predicate matches {@link com.sequenceiq.maintenance.service.model.WindowOccurrence#overlaps} half-open semantics:
     * {@code windowStart < dependentEnd && windowEnd > dependentStart}. {@code LessThan}/{@code GreaterThan} are strict —
     * do not widen to inclusive bounds without revisiting {@code WindowOccurrence}.
     * <p>
     * {@code OrderByWindowStartDesc} picks the most recently <em>started</em> overlapping row when several qualify
     * (e.g. schedule moved: older COMPLETED run still overlaps while a newer RUNNING run also overlaps). The newer
     * run is the current attempt; dependents wait on it rather than treating the stale completed row as satisfaction.
     */
    Optional<MaintenanceWindowRun> findFirstByMaintenanceWindowTaskIdAndWindowStartLessThanAndWindowEndGreaterThanOrderByWindowStartDesc(
            Long maintenanceWindowTaskId, Long windowStartLessThan, Long windowEndGreaterThan);

    List<MaintenanceWindowRun> findByMaintenanceWindowTaskId(Long maintenanceWindowTaskId);

    List<MaintenanceWindowRun> findByResourceCrn(String resourceCrn);

    List<MaintenanceWindowRun> findByAccountId(String accountId);

    List<MaintenanceWindowRun> findByStatus(MaintenanceRunStatus status);
}
