package com.sequenceiq.maintenance.api.model;

public enum MaintenanceScopeType {
    ENVIRONMENT,
    TENANT,
    DATAHUB,
    DATALAKE,
    FREEIPA;

    /**
     * Order for implicit platform sequencing (FreeIPA → Datalake → Datahub). Tasks are registered against
     * component resources only; ENVIRONMENT and TENANT are schedule scopes, not dispatchable task scopes.
     */
    public int implicitPlatformOrder() {
        return switch (this) {
            case FREEIPA -> 0;
            case DATALAKE -> 1;
            case DATAHUB -> 2;
            case ENVIRONMENT, TENANT -> throw new IllegalStateException(
                    this + " scope does not participate in implicit platform ordering.");
        };
    }
}
