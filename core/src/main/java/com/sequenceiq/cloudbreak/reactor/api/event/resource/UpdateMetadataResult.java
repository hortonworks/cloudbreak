package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpdateMetadataResult extends AbstractClusterUpscaleResult<UpdateMetadataRequest> {

    private int failedHosts;

    public UpdateMetadataResult(UpdateMetadataRequest request, int failedHosts) {
        super(request);
        this.failedHosts = failedHosts;
    }

    public UpdateMetadataResult(String statusReason, Exception errorDetails, UpdateMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }

    public int getFailedHosts() {
        return failedHosts;
    }
}
