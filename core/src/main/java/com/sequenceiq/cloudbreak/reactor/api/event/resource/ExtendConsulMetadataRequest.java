package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class ExtendConsulMetadataRequest extends AbstractClusterBootstrapRequest {

    public ExtendConsulMetadataRequest(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId, upscaleCandidateAddresses);
    }

}
