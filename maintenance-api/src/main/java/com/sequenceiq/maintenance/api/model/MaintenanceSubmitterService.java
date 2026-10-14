package com.sequenceiq.maintenance.api.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Canonical {@code submitter_service} values: the services that register maintenance window tasks and expose an
 * execute endpoint for the dispatcher to call back. Shared by submitters and by the dispatcher's endpoint resolver so
 * the wire values have a single definition.
 * <p>
 * Each first-party submitter owns a fixed set of resource scopes; submitter clients validate {@code resourceCrn}
 * against those scopes before register. The maintenance service does not yet enforce submitter vs. scope on create.
 */
public enum MaintenanceSubmitterService {

    CLOUDBREAK("cloudbreak", EnumSet.of(MaintenanceScopeType.DATAHUB)),
    DATALAKE("datalake", EnumSet.of(MaintenanceScopeType.DATALAKE)),
    FREEIPA("freeipa", EnumSet.of(MaintenanceScopeType.FREEIPA));

    private final String serviceName;

    private final Set<MaintenanceScopeType> ownedResourceScopes;

    MaintenanceSubmitterService(String serviceName, Set<MaintenanceScopeType> ownedResourceScopes) {
        this.serviceName = serviceName;
        this.ownedResourceScopes = Set.copyOf(ownedResourceScopes);
    }

    public String serviceName() {
        return serviceName;
    }

    /**
     * @throws IllegalArgumentException if {@code resourceScope} belongs to a different submitter
     */
    public MaintenanceScopeType validateResourceScope(String resourceCrn, MaintenanceScopeType resourceScope) {
        if (!ownedResourceScopes.contains(resourceScope)) {
            throw new IllegalArgumentException(String.format(
                    "%s registers maintenance tasks for %s resources only, but resourceCrn %s resolves to scope %s",
                    serviceName, ownedResourceScopes, resourceCrn, resourceScope));
        }
        return resourceScope;
    }
}
