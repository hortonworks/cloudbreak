package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;

public class StopStartDownscaleDecommissionViaCMRequest extends ClusterPlatformRequest implements HostGroupPayload {

    private final String hostGroupName;

    private final Stack stack;

    private final Set<Long> instanceIdsToDecommission;

    public StopStartDownscaleDecommissionViaCMRequest(Stack stack, String hostGroupName, Set<Long> instanceIdsToDecommission) {
        super(stack.getId());
        this.hostGroupName = hostGroupName;
        this.stack = stack;
        this.instanceIdsToDecommission = instanceIdsToDecommission;
    }

    public Set<Long> getInstanceIdsToDecommission() {
        return instanceIdsToDecommission;
    }

    public Stack getStack() {
        return stack;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleDecommissionViaCMRequest{" +
                "stack=" + stack +
                ", instanceIdsToDecommission=" + instanceIdsToDecommission +
                '}';
    }

}
