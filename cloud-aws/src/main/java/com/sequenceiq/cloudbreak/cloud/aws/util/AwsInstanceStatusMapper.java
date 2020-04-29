package com.sequenceiq.cloudbreak.cloud.aws.util;

import java.util.Map;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class AwsInstanceStatusMapper {
    private static final Map<String, InstanceStatus> STATUS_MAP = Map.of(
            "stopped", InstanceStatus.STOPPED,
            "running", InstanceStatus.STARTED,
            "terminated", InstanceStatus.TERMINATED);

    private AwsInstanceStatusMapper() {
    }

    public static InstanceStatus getInstanceStatusByAwsStatus(String status) {
        return STATUS_MAP.getOrDefault(status.toLowerCase(), InstanceStatus.IN_PROGRESS);
    }
}
