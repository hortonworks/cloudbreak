package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;

public class StopStartDownscaleDecommissionViaCMRequest extends ClusterPlatformRequest implements HostGroupPayload {

    private final String hostGroupName;

    private final Set<Long> instanceIdsToDecommission;

    private final List<CloudInstance> runningInstancesWithServicesNotRunning;

    @JsonCreator
    public StopStartDownscaleDecommissionViaCMRequest(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("hostGroupName") String hostGroupName,
            @JsonProperty("instanceIdsToDecommission") Set<Long> instanceIdsToDecommission,
            @JsonProperty("runningInstancesWithServicesNotRunning") List<CloudInstance> runningInstancesWithServicesNotRunning) {
        super(resourceId);
        this.hostGroupName = hostGroupName;
        this.instanceIdsToDecommission = instanceIdsToDecommission;
        this.runningInstancesWithServicesNotRunning = runningInstancesWithServicesNotRunning;
    }

    public Set<Long> getInstanceIdsToDecommission() {
        return instanceIdsToDecommission;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    public List<CloudInstance> getRunningInstancesWithServicesNotRunning() {
        return runningInstancesWithServicesNotRunning;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleDecommissionViaCMRequest{" +
                ", instanceIdsToDecommission=" + instanceIdsToDecommission +
                '}';
    }

}
