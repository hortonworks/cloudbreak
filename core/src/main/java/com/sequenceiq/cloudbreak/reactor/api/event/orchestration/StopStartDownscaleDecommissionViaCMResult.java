package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;

public class StopStartDownscaleDecommissionViaCMResult extends ClusterPlatformResult<StopStartDownscaleDecommissionViaCMRequest> implements FlowPayload {

    private final Set<String> decommissionedHostFqdns;

    private final List<String> notDecommissionedHostFqdns;

    @JsonCreator
    public StopStartDownscaleDecommissionViaCMResult(
            @JsonProperty("request") StopStartDownscaleDecommissionViaCMRequest request,
            @JsonProperty("decommissionedHostFqdns") Set<String> decommissionedHostFqdns,
            @JsonProperty("notDecommissionedHostFqdns") List<String> notDecommissionedHostFqdns) {
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
