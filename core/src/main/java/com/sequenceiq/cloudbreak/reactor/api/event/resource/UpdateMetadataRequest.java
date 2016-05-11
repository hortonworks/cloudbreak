package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpdateMetadataRequest extends AbstractClusterUpscaleRequest {

    public UpdateMetadataRequest(Long stackId, String hostGroupName) {
        super(stackId, hostGroupName);
    }
}
