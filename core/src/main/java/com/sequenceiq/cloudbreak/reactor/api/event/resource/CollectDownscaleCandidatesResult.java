package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

public class CollectDownscaleCandidatesResult extends AbstractClusterScaleResult<CollectDownscaleCandidatesRequest> {

    private final Set<Long> privateIds;

    public CollectDownscaleCandidatesResult(CollectDownscaleCandidatesRequest request, Set<Long> privateIds) {
        super(request);
        this.privateIds = privateIds;
    }

    public CollectDownscaleCandidatesResult(String statusReason, Exception errorDetails, CollectDownscaleCandidatesRequest request) {
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
