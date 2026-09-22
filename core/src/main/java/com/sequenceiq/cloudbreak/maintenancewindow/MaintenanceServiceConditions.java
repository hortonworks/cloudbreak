package com.sequenceiq.cloudbreak.maintenancewindow;

public final class MaintenanceServiceConditions {

    public static final String URL_PROPERTY = "cb.maintenance.url";

    public static final String URL_CONFIGURED = "!'${" + URL_PROPERTY + ":}'.isBlank()";

    private MaintenanceServiceConditions() {
    }
}
