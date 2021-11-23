package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StopStartDownscaleDecommissionViaCMResult extends AbstractClusterScaleResult<StopStartDownscaleDecommissionViaCMRequest> {

    private final Set<String> decommissionedHostFqdns;

    public StopStartDownscaleDecommissionViaCMResult(StopStartDownscaleDecommissionViaCMRequest request, Set<String> decommissionedHostFqdns) {
        super(request);
        this.decommissionedHostFqdns = decommissionedHostFqdns;
    }

    public Set<String> getDecommissionedHostFqdns() {
        return decommissionedHostFqdns;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleDecommissionViaCMResult{" +
                "decommissionedHostFqdns=" + decommissionedHostFqdns +
                '}';
    }
}