package com.sequenceiq.maintenance.service.model;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/**
 * Resource identity used to resolve the effective maintenance schedule.
 * Resolution order (most specific wins): resource scope → environment → tenant.
 */
public record MaintenanceWindowResourceIdentity(
        String accountId,
        String environmentCrn,
        String resourceCrn,
        MaintenanceScopeType resourceScopeType) {
}
