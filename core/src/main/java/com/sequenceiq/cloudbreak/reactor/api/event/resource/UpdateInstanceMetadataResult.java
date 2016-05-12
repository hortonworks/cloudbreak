package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpdateInstanceMetadataResult extends AbstractClusterScaleResult<UpdateInstanceMetadataRequest> {

    public UpdateInstanceMetadataResult(UpdateInstanceMetadataRequest request) {
        super(request);
    }

    public UpdateInstanceMetadataResult(String statusReason, Exception errorDetails, UpdateInstanceMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }
}
