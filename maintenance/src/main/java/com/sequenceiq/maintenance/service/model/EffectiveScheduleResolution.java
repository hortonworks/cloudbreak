package com.sequenceiq.maintenance.service.model;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;

/**
 * Winning schedule after inheritance resolution, plus the scope where it was defined.
 */
public record EffectiveScheduleResolution(
        MaintenanceWindowSchedule schedule,
        MaintenanceScopeType sourceScopeType,
        String sourceScopeId) {
}
