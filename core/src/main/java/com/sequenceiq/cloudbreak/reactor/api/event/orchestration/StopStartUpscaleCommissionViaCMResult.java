package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.ClusterPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;

public class StopStartUpscaleCommissionViaCMResult extends ClusterPlatformResult<StopStartUpscaleCommissionViaCMRequest> {

    private final Set<String> successfullyCommissionedFqdns;

    private final List<String> notRecommissionedFqdns;

    public StopStartUpscaleCommissionViaCMResult(StopStartUpscaleCommissionViaCMRequest request, Set<String> successfullyCommissionedFqdns,
            List<String> notCommissionedFqdns) {
        super(request);
        this.successfullyCommissionedFqdns = successfullyCommissionedFqdns;
        this.notRecommissionedFqdns = notCommissionedFqdns == null ? Collections.emptyList() : notCommissionedFqdns;
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
