package com.sequenceiq.maintenance.service.model;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/**
 * Validated inputs for effective-schedule resolution: resource scope and optional parent environment derived from the
 * resource (null when the owning service does not know the resource).
 */
public record EffectiveScheduleLookupContext(
        String accountId,
        MaintenanceScopeType resourceScopeType,
        String resourceCrn,
        String environmentCrn) {
}
