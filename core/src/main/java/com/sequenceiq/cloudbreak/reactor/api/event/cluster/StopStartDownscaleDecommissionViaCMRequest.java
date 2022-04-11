package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.HostGroupPayload;

public class StopStartDownscaleDecommissionViaCMRequest extends ClusterPlatformRequest implements HostGroupPayload {

    private final String hostGroupName;

    private final Set<Long> instanceIdsToDecommission;

    public StopStartDownscaleDecommissionViaCMRequest(Long resourceId, String hostGroupName, Set<Long> instanceIdsToDecommission) {
        super(resourceId);
        this.hostGroupName = hostGroupName;
        this.instanceIdsToDecommission = instanceIdsToDecommission;
    }

    public Set<Long> getInstanceIdsToDecommission() {
        return instanceIdsToDecommission;
    }

    @Override
    public String getHostGroupName() {
        return hostGroupName;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleDecommissionViaCMRequest{" +
                ", instanceIdsToDecommission=" + instanceIdsToDecommission +
                '}';
    }

}
