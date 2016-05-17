package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class ExtendConsulMetadataResult extends AbstractClusterBootstrapResult<ExtendConsulMetadataRequest> {
    public ExtendConsulMetadataResult(ExtendConsulMetadataRequest request) {
        super(request);
    }

    public ExtendConsulMetadataResult(String statusReason, Exception errorDetails, ExtendConsulMetadataRequest request) {
        super(statusReason, errorDetails, request);
    }
}
