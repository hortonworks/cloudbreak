package com.sequenceiq.freeipa.maintenancewindow;

public final class MaintenanceServiceConditions {

    public static final String URL_PROPERTY = "freeipa.maintenance.url";

    public static final String URL_CONFIGURED = "!'${" + URL_PROPERTY + ":}'.isBlank()";

    private MaintenanceServiceConditions() {
    }
}
