package com.sequenceiq.maintenance.service;

import java.util.Optional;

import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

/**
 * Effective schedule resolution for a resource at a point in time.
 * Maintenance is opt-in: {@link #dispatchable()} is false when no schedule applies or the current window is inactive/skipped.
 */
public record MaintenanceWindowScheduleEligibility(
        Optional<MaintenanceWindowSchedule> schedule,
        Optional<WindowOccurrence> currentOccurrence,
        boolean dispatchable) {

    public static MaintenanceWindowScheduleEligibility notDispatchable() {
        return new MaintenanceWindowScheduleEligibility(Optional.empty(), Optional.empty(), false);
    }

    public static MaintenanceWindowScheduleEligibility withoutActiveOccurrence(MaintenanceWindowSchedule schedule) {
        return new MaintenanceWindowScheduleEligibility(Optional.of(schedule), Optional.empty(), false);
    }

    public static MaintenanceWindowScheduleEligibility dispatchable(MaintenanceWindowSchedule schedule, WindowOccurrence occurrence) {
        return new MaintenanceWindowScheduleEligibility(Optional.of(schedule), Optional.of(occurrence), true);
    }
}
