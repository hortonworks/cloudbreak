package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Collections;
import java.util.Set;

public class DecommissionResult extends AbstractClusterScaleResult<DecommissionRequest> {

    private final Set<String> hostNames;

    public DecommissionResult(DecommissionRequest request, Set<String> hostNames) {
        super(request);
        this.hostNames = hostNames;
    }

    public DecommissionResult(String statusReason, Exception errorDetails, DecommissionRequest request) {
        super(statusReason, errorDetails, request);
        this.hostNames = Collections.emptySet();
    }

    public Set<String> getHostNames() {
        return hostNames;
    }

    @Override
    public String toString() {
        return "DecommissionResult{"
                + "hostNames=" + hostNames
                + '}';
    }
}
