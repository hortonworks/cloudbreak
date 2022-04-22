package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.StateReason;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class AwsInstanceStatusMapper {

    private AwsInstanceStatusMapper() {
    }

    public static InstanceStatus getInstanceStatusByAwsStatus(String status) {
        return getInstanceStatusByAwsStateAndReason(new InstanceState().withName(status), null);
    }

    public static InstanceStatus getInstanceStatusByAwsStateAndReason(InstanceState state, StateReason stateReason) {
        switch (state.getName().toLowerCase()) {
            case "stopped":
                return InstanceStatus.STOPPED;
            case "running":
                return InstanceStatus.STARTED;
            case "terminated":
                return stateReason != null && "Server.SpotInstanceTermination".equals(stateReason.getCode())
                        ? InstanceStatus.TERMINATED_BY_PROVIDER
                        : InstanceStatus.TERMINATED;
            default:
                return InstanceStatus.IN_PROGRESS;
        }
    }
}
