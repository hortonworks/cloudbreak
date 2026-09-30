package com.sequenceiq.maintenance.authorization;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

/**
 * Identifies the authorization subject of a schedule. Used to deduplicate scopes before batching UMS right checks, so
 * several schedules sharing a scope cost one check rather than one each.
 */
public record MaintenanceScopeKey(MaintenanceScopeType scopeType, String scopeId) {
}
