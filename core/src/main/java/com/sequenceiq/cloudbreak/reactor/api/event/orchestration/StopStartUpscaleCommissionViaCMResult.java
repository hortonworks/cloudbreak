package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;
import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;

public class StopStartUpscaleCommissionViaCMResult extends ClusterPlatformResult<StopStartUpscaleCommissionViaCMRequest> implements FlowPayload {

    private final Set<String> successfullyCommissionedFqdns;

    private final List<String> notRecommissionedFqdns;

    @JsonCreator
    public StopStartUpscaleCommissionViaCMResult(
            @JsonProperty("request") StopStartUpscaleCommissionViaCMRequest request,
            @JsonProperty("successfullyCommissionedFqdns") Set<String> successfullyCommissionedFqdns,
            @JsonProperty("notRecommissionedFqdns") List<String> notRecommissionedFqdns) {
        super(request);
        this.successfullyCommissionedFqdns = successfullyCommissionedFqdns;
        this.notRecommissionedFqdns = notRecommissionedFqdns == null ? Collections.emptyList() : notRecommissionedFqdns;
    }

    public StopStartUpscaleCommissionViaCMResult(String statusReason, Exception errorDetails, StopStartUpscaleCommissionViaCMRequest request) {
        super(statusReason, errorDetails, request);
        this.successfullyCommissionedFqdns = Collections.emptySet();
        this.notRecommissionedFqdns = Collections.emptyList();
    }

    public Set<String> getSuccessfullyCommissionedFqdns() {
        return successfullyCommissionedFqdns;
    }

    public List<String> getNotRecommissionedFqdns() {
        return notRecommissionedFqdns;
    }

    @Override
    public String toString() {
        return "StopStartUpscaleCommissionViaCMResult{" +
                "successfullyCommissionedFqdnsCount=" + successfullyCommissionedFqdns.size() +
                "notRecommissionedFqdnsCount=" + notRecommissionedFqdns.size() +
                '}';
    }
}
