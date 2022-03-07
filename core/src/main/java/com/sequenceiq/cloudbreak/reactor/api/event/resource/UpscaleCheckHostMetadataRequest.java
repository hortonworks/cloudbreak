package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

public class UpscaleCheckHostMetadataRequest extends AbstractClusterScaleRequest {

    private final String primaryGatewayHostname;

    private final boolean singlePrimaryGateway;

    public UpscaleCheckHostMetadataRequest(Long stackId, Set<String> hostGroups, String primaryGatewayHostname, boolean singlePrimaryGateway) {
        super(stackId, hostGroups);
        this.primaryGatewayHostname = primaryGatewayHostname;
        this.singlePrimaryGateway = singlePrimaryGateway;
    }

    public String getPrimaryGatewayHostname() {
        return primaryGatewayHostname;
    }

    public boolean isSinglePrimaryGateway() {
        return singlePrimaryGateway;
    }
}
