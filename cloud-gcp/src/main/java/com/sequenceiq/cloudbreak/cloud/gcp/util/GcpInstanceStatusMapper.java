package com.sequenceiq.cloudbreak.cloud.gcp.util;

import java.util.Locale;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class GcpInstanceStatusMapper {

    private GcpInstanceStatusMapper() {
    }

    public static InstanceStatus getInstanceStatusFromGcpStatus(String status) {
        String formattedStatus = status == null ? "" : status.toUpperCase(Locale.ROOT);
        return switch (formattedStatus) {
            case "RUNNING" -> InstanceStatus.STARTED;
            case "TERMINATED" -> InstanceStatus.STOPPED;
            default -> InstanceStatus.IN_PROGRESS;
        };
    }
}
