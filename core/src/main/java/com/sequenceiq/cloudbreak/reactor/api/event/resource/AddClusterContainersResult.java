package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class AddClusterContainersResult extends AbstractClusterUpscaleResult<AddClusterContainersRequest> {

    public AddClusterContainersResult(AddClusterContainersRequest request) {
        super(request);
    }

    public AddClusterContainersResult(String statusReason, Exception errorDetails, AddClusterContainersRequest request) {
        super(statusReason, errorDetails, request);
    }
}
