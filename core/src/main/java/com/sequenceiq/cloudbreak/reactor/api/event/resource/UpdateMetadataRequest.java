package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpdateMetadataRequest extends AbstractClusterScaleRequest {

    public UpdateMetadataRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
