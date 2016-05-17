package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class WaitForAmbariHostsResult extends AbstractClusterScaleResult<WaitForAmbariHostsRequest> {

    public WaitForAmbariHostsResult(WaitForAmbariHostsRequest request) {
        super(request);
    }

    public WaitForAmbariHostsResult(String statusReason, Exception errorDetails, WaitForAmbariHostsRequest request) {
        super(statusReason, errorDetails, request);
    }
}
