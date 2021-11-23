package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;


public class StopStartDownscaleDecommissionViaCMRequest extends AbstractClusterScaleRequest {

    private final Stack stack;

    private final Set<Long> instanceIdsToDecommission;

    public StopStartDownscaleDecommissionViaCMRequest(Stack stack, String hostGroupName, Set<Long> instanceIdsToDecommission) {
        super(stack.getId(), hostGroupName);
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
    public String toString() {
        return "StopStartDownscaleDecommissionViaCMRequest{" +
                "stack=" + stack +
                ", instanceIdsToDecommission=" + instanceIdsToDecommission +
                '}';
    }
}
