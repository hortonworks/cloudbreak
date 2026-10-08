package com.sequenceiq.maintenance.api.model;

/**
 * Canonical {@code submitter_service} values: the services that register maintenance window tasks and expose an
 * execute endpoint for the dispatcher to call back. Shared by submitters and by the dispatcher's endpoint resolver so
 * the wire values have a single definition.
 */
public enum MaintenanceSubmitterService {

    CLOUDBREAK("cloudbreak"),
    DATALAKE("datalake"),
    FREEIPA("freeipa");

    private final String serviceName;

    MaintenanceSubmitterService(String serviceName) {
        this.serviceName = serviceName;
    }

    public String serviceName() {
        return serviceName;
    }
}
