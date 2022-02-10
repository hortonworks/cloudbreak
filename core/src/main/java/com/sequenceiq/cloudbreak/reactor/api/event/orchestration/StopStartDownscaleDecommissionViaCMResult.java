package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleResult;

public class StopStartDownscaleDecommissionViaCMResult extends AbstractClusterScaleResult<StopStartDownscaleDecommissionViaCMRequest> {

    private final Set<String> decommissionedHostFqdns;

    private final List<String> notDecommissionedHostFqdns;

    public StopStartDownscaleDecommissionViaCMResult(StopStartDownscaleDecommissionViaCMRequest request, Set<String> decommissionedHostFqdns,
            List<String> notDecommissionedHostFqdns) {
        super(request);
        this.decommissionedHostFqdns = decommissionedHostFqdns;
        this.notDecommissionedHostFqdns = notDecommissionedHostFqdns == null ? Collections.emptyList() : notDecommissionedHostFqdns;
    }

    public StopStartDownscaleDecommissionViaCMResult(String statusReason, Exception errorDetails, StopStartDownscaleDecommissionViaCMRequest request) {
        super(statusReason, errorDetails, request);
        this.decommissionedHostFqdns = Collections.emptySet();
        this.notDecommissionedHostFqdns = Collections.emptyList();
    }

    public Set<String> getDecommissionedHostFqdns() {
        return decommissionedHostFqdns;
    }

    public List<String> getNotDecommissionedHostFqdns() {
        return notDecommissionedHostFqdns;
    }

    @Override
    public String toString() {
        return "StopStartDownscaleDecommissionViaCMResult{" +
                "decommissionedHostFqdns=" + decommissionedHostFqdns +
                ", notDecommissionedHostFqdns=" + notDecommissionedHostFqdns +
                '}';
    }
}