package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance.MaintenanceModeValidationService.WarningType;

public class Warning {

    private final WarningType warningType;

    private final String message;

    public Warning(WarningType warningType, String message) {
        this.warningType = warningType;
        this.message = message;
    }

    public WarningType getWarningType() {
        return warningType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Warning{");
        sb.append("warningType=").append(warningType);
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
