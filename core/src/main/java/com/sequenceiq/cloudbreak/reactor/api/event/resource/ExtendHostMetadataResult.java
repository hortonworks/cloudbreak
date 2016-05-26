package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExtendHostMetadataResult extends AbstractClusterBootstrapResult<ExtendHostMetadataRequest> {
    public ExtendHostMetadataResult(ExtendHostMetadataRequest request) {
        super(request);
    }

    public ExtendHostMetadataResult(String statusReason, Exception errorDetails, ExtendHostMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }
}
