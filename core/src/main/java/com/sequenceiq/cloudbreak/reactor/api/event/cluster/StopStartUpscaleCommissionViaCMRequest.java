package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class StopStartUpscaleCommissionViaCMRequest extends AbstractClusterScaleRequest {

    private List<InstanceMetaData> instancesToCommission;

    private final Stack stack;

    public StopStartUpscaleCommissionViaCMRequest(Stack stack, String hostGroupName, List<InstanceMetaData> instanceList) {
        super(stack.getId(), hostGroupName);
        this.stack = stack;
        this.instancesToCommission = instanceList;
    }

    public List<InstanceMetaData> getInstancesToCommission() {
        return instancesToCommission;
    }

    public Stack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleCommissionViaCMRequest{" +
                "instancesToCommission=" + instancesToCommission +
                ", stack=" + stack +
                '}';
    }
}
