package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

public class CollectDownscaleCandidatesResult extends AbstractClusterScaleResult<CollectDownscaleCandidatesRequest> {

    private final Set<String> hostNames;

    public CollectDownscaleCandidatesResult(CollectDownscaleCandidatesRequest request, Set<String> hostNames) {
        super(request);
        this.hostNames = hostNames;
    }

    public CollectDownscaleCandidatesResult(String statusReason, Exception errorDetails, CollectDownscaleCandidatesRequest request) {
        super(statusReason, errorDetails, request);
        hostNames = Collections.emptySet();
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    @Override
    public String toString() {
        return "CollectDownscaleCandidatesResult{"
                + "hostNames=" + hostNames
                + '}';
    }
}
