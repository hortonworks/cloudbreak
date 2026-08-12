package com.sequenceiq.maintenance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

@Service
public class MaintenanceWindowScheduleEligibilityService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowScheduleEligibilityService.class);

    private final MaintenanceWindowScheduleRepository scheduleRepository;

    private final MaintenanceWindowSkipRepository skipRepository;

    private final MaintenanceOccurrenceCalculator occurrenceCalculator;

    private final Clock clock;

    @Inject
    public MaintenanceWindowScheduleEligibilityService(
            MaintenanceWindowScheduleRepository scheduleRepository,
            MaintenanceWindowSkipRepository skipRepository,
            MaintenanceOccurrenceCalculator occurrenceCalculator,
            Clock clock) {
        this.scheduleRepository = scheduleRepository;
        this.skipRepository = skipRepository;
        this.occurrenceCalculator = occurrenceCalculator;
        this.clock = clock;
    }

    public MaintenanceWindowScheduleEligibility checkEligibility(MaintenanceWindowResourceIdentity identity) {
        return checkEligibility(identity, clock.getCurrentTimeMillis());
    }

    public MaintenanceWindowScheduleEligibility checkEligibility(MaintenanceWindowResourceIdentity identity, long nowMs) {
        Optional<MaintenanceWindowSchedule> schedule = resolveEffectiveSchedule(identity);
        if (schedule.isEmpty()) {
            LOGGER.debug("Maintenance window not dispatchable: no applicable schedule");
            return MaintenanceWindowScheduleEligibility.notDispatchable();
        }
        MaintenanceWindowSchedule winningSchedule = schedule.get();
        Optional<WindowOccurrence> matchingOccurrence = occurrenceCalculator.findOccurrenceContaining(winningSchedule, nowMs);
        if (matchingOccurrence.isEmpty()) {
            LOGGER.debug("Maintenance window not dispatchable: schedule id={} scopeType={} has no active occurrence",
                    winningSchedule.getId(), winningSchedule.getScopeType());
            return MaintenanceWindowScheduleEligibility.withoutActiveOccurrence(winningSchedule);
        }
        WindowOccurrence occurrence = matchingOccurrence.get();
        if (isSkipped(winningSchedule.getId(), occurrence)) {
            LOGGER.debug("Maintenance window not dispatchable: schedule id={} occurrence windowStart={} is skipped",
                    winningSchedule.getId(), occurrence.windowStart());
            return MaintenanceWindowScheduleEligibility.withoutActiveOccurrence(winningSchedule);
        }
        LOGGER.debug("Maintenance window dispatchable: schedule id={} scopeType={} windowStart={} windowEnd={}",
                winningSchedule.getId(), winningSchedule.getScopeType(), occurrence.windowStart(), occurrence.windowEnd());
        return MaintenanceWindowScheduleEligibility.dispatchable(winningSchedule, occurrence);
    }

    public Optional<MaintenanceWindowSchedule> resolveEffectiveSchedule(MaintenanceWindowResourceIdentity identity) {
        if (StringUtils.isBlank(identity.accountId())) {
            LOGGER.debug("Cannot resolve maintenance schedule: blank accountId");
            return Optional.empty();
        }
        List<ScopeCandidate> candidates = scopeCandidates(identity);
        for (ScopeCandidate candidate : candidates) {
            Optional<MaintenanceWindowSchedule> schedule = scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                    identity.accountId(), candidate.scopeType(), candidate.scopeId());
            if (schedule.isPresent()) {
                LOGGER.debug("Resolved maintenance schedule id={} scopeType={}",
                        schedule.get().getId(), schedule.get().getScopeType());
                return schedule;
            }
        }
        LOGGER.debug("No maintenance schedule matched among {} scope candidates", candidates.size());
        return Optional.empty();
    }

    private List<ScopeCandidate> scopeCandidates(MaintenanceWindowResourceIdentity identity) {
        List<ScopeCandidate> candidates = new ArrayList<>();
        if (identity.resourceScopeType() != null && StringUtils.isNotBlank(identity.resourceCrn())) {
            candidates.add(new ScopeCandidate(identity.resourceScopeType(), identity.resourceCrn()));
        }
        if (StringUtils.isNotBlank(identity.environmentCrn())) {
            candidates.add(new ScopeCandidate(MaintenanceScopeType.ENVIRONMENT, identity.environmentCrn()));
        }
        candidates.add(new ScopeCandidate(MaintenanceScopeType.TENANT, identity.accountId()));
        return candidates;
    }

    private boolean isSkipped(Long scheduleId, WindowOccurrence occurrence) {
        return skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(scheduleId, occurrence.windowStart()).isPresent();
    }

    private record ScopeCandidate(MaintenanceScopeType scopeType, String scopeId) {
    }
}
