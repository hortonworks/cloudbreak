package com.sequenceiq.maintenance.service;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSkipRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowSkipResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

@Service
public class MaintenanceWindowSkipService {

    private final MaintenanceWindowScheduleService scheduleService;

    private final MaintenanceWindowSkipRepository skipRepository;

    private final MaintenanceWindowScheduleConverter scheduleConverter;

    private final MaintenanceOccurrenceCalculator occurrenceCalculator;

    private final Clock clock;

    @Inject
    public MaintenanceWindowSkipService(
            MaintenanceWindowScheduleService scheduleService,
            MaintenanceWindowSkipRepository skipRepository,
            MaintenanceWindowScheduleConverter scheduleConverter,
            MaintenanceOccurrenceCalculator occurrenceCalculator,
            Clock clock) {
        this.scheduleService = scheduleService;
        this.skipRepository = skipRepository;
        this.scheduleConverter = scheduleConverter;
        this.occurrenceCalculator = occurrenceCalculator;
        this.clock = clock;
    }

    /**
     * Records a skip for the earliest not-yet-started occurrence ({@code windowStart > now}) with {@code windowEnd > now}.
     * Returns 409 when that occurrence is already skipped or the current window is already in progress.
     */
    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowSkipResponse skipNextWindow(
            String accountId, MaintenanceScopeType scopeType, String scopeId,
            MaintenanceWindowSkipRequest request, String userCrn) {
        MaintenanceWindowSchedule schedule = scheduleService.findRequired(accountId, scopeType, scopeId);
        WindowOccurrence nextOccurrence = resolveNextSkippableOccurrence(schedule);

        skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(schedule.getId(), nextOccurrence.windowStart())
                .ifPresent(existing -> {
                    throw occurrenceAlreadySkippedException();
                });

        MaintenanceWindowSkip skip = new MaintenanceWindowSkip();
        skip.setMaintenanceWindowSchedule(schedule);
        skip.setWindowStart(nextOccurrence.windowStart());
        skip.setWindowEnd(nextOccurrence.windowEnd());
        skip.setTimezone(schedule.getTimezone());
        skip.setCreatedAt(clock.getCurrentTimeMillis());
        skip.setCreatedBy(userCrn);
        skip.setReason(request != null ? request.getReason() : null);
        try {
            return scheduleConverter.toSkipResponse(skipRepository.save(skip));
        } catch (DataIntegrityViolationException | ConstraintViolationException e) {
            throw occurrenceAlreadySkippedException(e);
        }
    }

    /**
     * Removes the skip for the window covering {@code now} if present, otherwise the earliest skip with
     * {@code windowEnd > now} (which may be an in-progress window).
     */
    @Transactional(TxType.REQUIRED)
    public MaintenanceWindowSkipResponse cancelSkipNextWindow(String accountId, MaintenanceScopeType scopeType, String scopeId) {
        MaintenanceWindowSchedule schedule = scheduleService.findRequired(accountId, scopeType, scopeId);
        MaintenanceWindowSkip skip = findCancellableSkip(schedule.getId(), scopeType, scopeId);
        MaintenanceWindowSkipResponse response = scheduleConverter.toSkipResponse(skip);
        skipRepository.delete(skip);
        return response;
    }

    private MaintenanceWindowSkip findCancellableSkip(Long scheduleId, MaintenanceScopeType scopeType, String scopeId) {
        long now = clock.getCurrentTimeMillis();
        List<MaintenanceWindowSkip> skips = skipRepository.findByMaintenanceWindowScheduleId(scheduleId);

        Optional<MaintenanceWindowSkip> coveringNow = skips.stream()
                .filter(skip -> skip.getWindowStart() <= now && now < skip.getWindowEnd())
                .findFirst();
        if (coveringNow.isPresent()) {
            return coveringNow.get();
        }

        return skips.stream()
                .filter(skip -> skip.getWindowEnd() > now)
                .min(Comparator.comparingLong(MaintenanceWindowSkip::getWindowStart))
                .orElseThrow(notFound(String.format(
                        "No skip found for an upcoming maintenance window at scope %s/%s.",
                        scopeType, scopeId)));
    }

    private WindowOccurrence resolveNextSkippableOccurrence(MaintenanceWindowSchedule schedule) {
        long now = clock.getCurrentTimeMillis();
        WindowOccurrence nextOccurrence = occurrenceCalculator.findNextUpcomingOccurrence(schedule, now)
                .orElseThrow(() -> new BadRequestException("No upcoming maintenance window occurrence found within the next 90 days."));
        if (nextOccurrence.windowStart() <= now) {
            throw maintenanceWindowInProgressException();
        }
        return nextOccurrence;
    }

    private ConflictException occurrenceAlreadySkippedException() {
        return occurrenceAlreadySkippedException(null);
    }

    private ConflictException occurrenceAlreadySkippedException(Throwable cause) {
        String message = "The next maintenance window occurrence is already skipped.";
        return cause == null ? new ConflictException(message) : new ConflictException(message, cause);
    }

    private ConflictException maintenanceWindowInProgressException() {
        return new ConflictException("Maintenance window is already in progress.");
    }
}
