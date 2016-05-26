package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class ExtendHostMetadataRequest extends AbstractClusterBootstrapRequest {

    public ExtendHostMetadataRequest(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId, upscaleCandidateAddresses);
    }

}
