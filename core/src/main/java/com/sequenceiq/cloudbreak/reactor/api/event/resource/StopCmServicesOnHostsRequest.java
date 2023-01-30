package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.sequenceiq.cloudbreak.core.flow2.event.ClusterDownscaleDetails;

public class StopCmServicesOnHostsRequest extends DecommissionRequest {

    private final Set<String> hostNamesToStop;

    public StopCmServicesOnHostsRequest(Long stackId, Set<String> hostGroups, Set<String> hostNamesToStop, Set<Long> privateIds,
            ClusterDownscaleDetails details) {
        super(stackId, hostGroups, privateIds, details);
        this.hostNamesToStop = hostNamesToStop;
    }

    public Set<String> getHostNamesToStop() {
        return hostNamesToStop;
    }

    @Override
    public String toString() {
        return "StopCmServicesOnHostsRequest{" +
                "hostNamesToStop=" + hostNamesToStop +
                "} " + super.toString();
    }
}
