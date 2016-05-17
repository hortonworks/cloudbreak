package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpdateMetadataResult extends AbstractClusterScaleResult<UpdateMetadataRequest> {

    private final int failedHosts;

    public UpdateMetadataResult(UpdateMetadataRequest request, int failedHosts) {
        super(request);
        this.failedHosts = failedHosts;
    }

    public UpdateMetadataResult(String statusReason, Exception errorDetails, UpdateMetadataRequest request) {
        super(statusReason, errorDetails, request);
        this.failedHosts = -1;
    }

    public int getFailedHosts() {
        return failedHosts;
    }
}
