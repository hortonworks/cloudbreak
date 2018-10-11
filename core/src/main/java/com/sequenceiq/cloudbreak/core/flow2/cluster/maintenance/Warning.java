package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

public class Warning {

    private final MaintenanceModeValidationService.WarningType warningType;

    private String message;

    public Warning(MaintenanceModeValidationService.WarningType warningType, String message) {
        this.warningType = warningType;
        this.message = message;
    }

    public MaintenanceModeValidationService.WarningType getWarningType() {
        return warningType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Warning{");
        sb.append("warningType=").append(warningType);
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
