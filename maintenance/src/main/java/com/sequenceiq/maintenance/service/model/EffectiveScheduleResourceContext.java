package com.sequenceiq.maintenance.service.model;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/** Local validation result for effective-schedule lookup (no cross-service calls). */
public record EffectiveScheduleResourceContext(
        String accountId,
        MaintenanceScopeType resourceScopeType,
        String resourceCrn) {
}
