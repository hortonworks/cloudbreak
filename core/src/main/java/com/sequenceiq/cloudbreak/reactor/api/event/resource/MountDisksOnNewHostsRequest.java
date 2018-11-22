package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class MountDisksOnNewHostsRequest extends AbstractClusterBootstrapRequest {

    public MountDisksOnNewHostsRequest(Long stackId, Set<String> upscaleCandidateAddresses) {
        super(stackId, upscaleCandidateAddresses);
    }

}
