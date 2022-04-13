package com.sequenceiq.cloudbreak.cloud.aws.common.util;

import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.StateReason;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;

public class AwsInstanceStatusMapper {

    public static final String RUNNING = "running";

    public static final String STOPPED = "stopped";

    public static final String TERMINATED = "terminated";

    private AwsInstanceStatusMapper() {
    }

    public static InstanceStatus getInstanceStatusByAwsStatus(String status) {
        return getInstanceStatusByAwsStateAndReason(new InstanceState().withName(status), null);
    }

    public static InstanceStatus getInstanceStatusByAwsStateAndReason(InstanceState state, StateReason stateReason) {
        switch (state.getName().toLowerCase()) {
            case STOPPED:
                return InstanceStatus.STOPPED;
            case RUNNING:
                return InstanceStatus.STARTED;
            case TERMINATED:
                return stateReason != null && "Server.SpotInstanceTermination".equals(stateReason.getCode())
                        ? InstanceStatus.TERMINATED_BY_PROVIDER
                        : InstanceStatus.TERMINATED;
            default:
                return InstanceStatus.IN_PROGRESS;
        }
    }

    public static boolean isTerminated(InstanceState instanceState) {
        return TERMINATED.equalsIgnoreCase(instanceState.getName());
    }
}
