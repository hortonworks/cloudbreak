package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.List;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class StopStartUpscaleCommissionViaCMRequest extends AbstractClusterScaleRequest {

    private final List<InstanceMetaData> startedInstancesToCommission;

    private final List<InstanceMetaData> servicesNotRunningInstancesToCommission;

    private final Stack stack;

    public StopStartUpscaleCommissionViaCMRequest(Stack stack, String hostGroupName,
            List<InstanceMetaData> startedInstancesToCommission,
            List<InstanceMetaData> servicesNotRunningInstancesToCommission) {
        super(stack.getId(), hostGroupName);
        this.stack = stack;
        this.startedInstancesToCommission = startedInstancesToCommission;
        this.servicesNotRunningInstancesToCommission = servicesNotRunningInstancesToCommission;
    }

    public List<InstanceMetaData> getStartedInstancesToCommission() {
        return startedInstancesToCommission;
    }

    public List<InstanceMetaData> getServicesNotRunningInstancesToCommission() {
        return servicesNotRunningInstancesToCommission;
    }

    public Stack getStack() {
        return stack;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleCommissionViaCMRequest{" +
                "startedInstancesToCommissionCount=" + startedInstancesToCommission.size() +
                ", servicesNotRunningInstancesToCommissionCount=" + servicesNotRunningInstancesToCommission.size() +
                ", stack=" + stack +
                '}';
    }
}
