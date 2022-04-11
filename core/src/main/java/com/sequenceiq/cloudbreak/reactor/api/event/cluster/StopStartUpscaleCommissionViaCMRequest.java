package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Collections;
import java.util.List;

import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;

public class StopStartUpscaleCommissionViaCMRequest extends ClusterPlatformRequest implements HostGroupPayload {

    private final String hostGroupName;

    private final List<InstanceMetaData> startedInstancesToCommission;

    private final List<InstanceMetaData> servicesNotRunningInstancesToCommission;

    public StopStartUpscaleCommissionViaCMRequest(Long stackId, String hostGroupName, List<InstanceMetaData> startedInstancesToCommission,
            List<InstanceMetaData> servicesNotRunningInstancesToCommission) {
        super(stackId);
        this.hostGroupName = hostGroupName;
        this.startedInstancesToCommission = startedInstancesToCommission == null ? Collections.emptyList() : startedInstancesToCommission;
        this.servicesNotRunningInstancesToCommission =
                servicesNotRunningInstancesToCommission == null ? Collections.emptyList() : servicesNotRunningInstancesToCommission;
    }

    public List<InstanceMetaData> getStartedInstancesToCommission() {
        return startedInstancesToCommission;
    }

    public List<InstanceMetaData> getServicesNotRunningInstancesToCommission() {
        return servicesNotRunningInstancesToCommission;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleCommissionViaCMRequest{" +
                "startedInstancesToCommissionCount=" + startedInstancesToCommission.size() +
                ", servicesNotRunningInstancesToCommissionCount=" + servicesNotRunningInstancesToCommission.size() +
                '}';
    }

}
