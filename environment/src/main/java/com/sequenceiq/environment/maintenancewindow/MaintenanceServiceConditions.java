package com.sequenceiq.environment.maintenancewindow;

public final class MaintenanceServiceConditions {

    public static final String URL_PROPERTY = "environment.maintenance.url";

    public static final String URL_CONFIGURED = "!'${" + URL_PROPERTY + ":}'.isBlank()";

    public static final String NOT_CONFIGURED_VALIDATION_ERROR =
            "Maintenance window schedules require the maintenance service (environment.maintenance.url).";

    private MaintenanceServiceConditions() {
    }
}
