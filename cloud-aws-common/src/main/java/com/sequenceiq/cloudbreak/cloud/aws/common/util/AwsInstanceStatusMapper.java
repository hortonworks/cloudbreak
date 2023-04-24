package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.StateReason;

public class AwsInstanceStatusMapper {

    private AwsInstanceStatusMapper() {
    }

    public static InstanceStatus getInstanceStatusByAwsStatus(String status) {
        return getInstanceStatusByAwsStateAndReason(InstanceState.builder().name(status.toLowerCase()).build(), null);
    }

    public static InstanceStatus getInstanceStatusByAwsStateAndReason(InstanceState state, StateReason stateReason) {
        switch (state.nameAsString().toLowerCase()) {
            case "stopped":
                return InstanceStatus.STOPPED;
            case "running":
                return InstanceStatus.STARTED;
            case "shutting-down" :
                return InstanceStatus.SHUTTING_DOWN;
            case "pending" :
                return InstanceStatus.PENDING;
            case "terminated":
                return stateReason != null && "Server.SpotInstanceTermination".equals(stateReason.code())
                        ? InstanceStatus.TERMINATED_BY_PROVIDER
                        : InstanceStatus.TERMINATED;
            default:
                return InstanceStatus.IN_PROGRESS;
        }
    }
}
