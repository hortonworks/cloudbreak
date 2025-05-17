package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import java.util.Locale;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.StateReason;

public class AwsInstanceStatusMapper {

    private AwsInstanceStatusMapper() {
    }

    public static InstanceStatus getInstanceStatusByAwsStatus(String status) {
        return getInstanceStatusByAwsStateAndReason(InstanceState.builder().name(status.toLowerCase(Locale.ROOT)).build(), null);
    }

    public static InstanceStatus getInstanceStatusByAwsStateAndReason(InstanceState state, StateReason stateReason) {
        return switch (state.nameAsString().toLowerCase(Locale.ROOT)) {
            case "stopped" -> InstanceStatus.STOPPED;
            case "running" -> InstanceStatus.STARTED;
            case "shutting-down" -> InstanceStatus.SHUTTING_DOWN;
            case "terminated" -> stateReason != null && "Server.SpotInstanceTermination".equals(stateReason.code())
                    ? InstanceStatus.TERMINATED_BY_PROVIDER
                    : InstanceStatus.TERMINATED;
            default -> InstanceStatus.IN_PROGRESS;
        };
    }
}
