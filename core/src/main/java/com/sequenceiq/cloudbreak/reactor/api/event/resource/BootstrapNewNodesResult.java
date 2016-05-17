package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class BootstrapNewNodesResult extends AbstractClusterBootstrapResult<BootstrapNewNodesRequest> {
    public BootstrapNewNodesResult(BootstrapNewNodesRequest request) {
        super(request);
    }

    public BootstrapNewNodesResult(String statusReason, Exception errorDetails, BootstrapNewNodesRequest request) {
        super(statusReason, errorDetails, request);
    }
}
