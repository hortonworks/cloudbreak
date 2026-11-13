package com.sequenceiq.maintenance.service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSchedulePreviewRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSchedulePreviewResponse;
import com.sequenceiq.maintenance.domain.MaintenanceEnumValues;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

@Service
public class MaintenanceWindowSchedulePreviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceWindowSchedulePreviewService.class);

    private final MaintenanceWindowScheduleConverter scheduleConverter;

    private final MaintenanceWindowScheduleValidator scheduleValidator;

    private final MaintenanceOccurrenceCalculator occurrenceCalculator;

    private final MaintenanceWindowScheduleRepository scheduleRepository;

    private final MaintenanceWindowSkipRepository skipRepository;

    private final Clock clock;

    @Inject
    public MaintenanceWindowSchedulePreviewService(
            MaintenanceWindowScheduleConverter scheduleConverter,
            MaintenanceWindowScheduleValidator scheduleValidator,
            MaintenanceOccurrenceCalculator occurrenceCalculator,
            MaintenanceWindowScheduleRepository scheduleRepository,
            MaintenanceWindowSkipRepository skipRepository,
            Clock clock) {
        this.scheduleConverter = scheduleConverter;
        this.scheduleValidator = scheduleValidator;
        this.occurrenceCalculator = occurrenceCalculator;
        this.scheduleRepository = scheduleRepository;
        this.skipRepository = skipRepository;
        this.clock = clock;
    }

    public MaintenanceWindowSchedulePreviewResponse preview(MaintenanceWindowSchedulePreviewRequest request, String accountId) {
        boolean scopeProvided = StringUtils.isNotBlank(request.getScopeType()) && StringUtils.isNotBlank(request.getScopeId());
        Optional<MaintenanceWindowSchedule> scheduleAtScope = findScheduleAtScope(request, accountId, scopeProvided);
        MaintenanceWindowSchedule schedule = scheduleConverter.toPreviewEntity(request, accountId);
        scheduleValidator.validatePreview(schedule, scopeProvided);
        long now = clock.getCurrentTimeMillis();
        List<WindowOccurrence> occurrences = occurrenceCalculator.listUpcomingOccurrences(schedule, now).stream()
                .limit(request.resolvedLimit())
                .toList();
        Set<Long> skippedWindowStarts = resolveSkippedWindowStarts(request, scheduleAtScope);
        LOGGER.debug(
                "Maintenance schedule preview for accountId={} scopeType={} scopeId={} limit={} includePersistedSkips={}"
                        + " persistedScheduleAtScope={} occurrenceCount={} skippedOccurrenceCount={}",
                accountId,
                request.getScopeType(),
                request.getScopeId(),
                request.resolvedLimit(),
                request.isIncludePersistedSkips(),
                scheduleAtScope.isPresent(),
                occurrences.size(),
                skippedWindowStarts.size());
        return scheduleConverter.toPreviewResponse(schedule, occurrences, skippedWindowStarts);
    }

    private Optional<MaintenanceWindowSchedule> findScheduleAtScope(
            MaintenanceWindowSchedulePreviewRequest request, String accountId, boolean scopeProvided) {
        if (!scopeProvided) {
            return Optional.empty();
        }
        MaintenanceScopeType scopeType = MaintenanceEnumValues.toScopeType(request.getScopeType());
        return scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                accountId, scopeType, request.getScopeId());
    }

    private Set<Long> resolveSkippedWindowStarts(
            MaintenanceWindowSchedulePreviewRequest request, Optional<MaintenanceWindowSchedule> scheduleAtScope) {
        if (!request.isIncludePersistedSkips() || scheduleAtScope.isEmpty()) {
            return Set.of();
        }
        Set<Long> skipped = new HashSet<>();
        for (MaintenanceWindowSkip skip : skipRepository.findByMaintenanceWindowScheduleId(scheduleAtScope.get().getId())) {
            skipped.add(skip.getWindowStart());
        }
        return skipped;
    }
}
