package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.FlowPayload;

public class CollectDownscaleCandidatesResult extends AbstractClusterScaleResult<CollectDownscaleCandidatesRequest> implements FlowPayload {

    private final Set<Long> privateIds;

    public CollectDownscaleCandidatesResult(CollectDownscaleCandidatesRequest request, Set<Long> privateIds) {
        super(request);
        this.privateIds = privateIds;
    }

    @JsonCreator
    public CollectDownscaleCandidatesResult(
            @JsonProperty("statusReason") String statusReason,
            @JsonProperty("errorDetails") Exception errorDetails,
            @JsonProperty("request") CollectDownscaleCandidatesRequest request) {
        super(statusReason, errorDetails, request);
        privateIds = Collections.emptySet();
    }

    public Set<Long> getPrivateIds() {
        return privateIds;
    }

    @Override
    public String toString() {
        return "CollectDownscaleCandidatesResult{"
                + "privateIds=" + privateIds
                + '}';
    }
}
