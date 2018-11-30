package com.sequenceiq.cloudbreak.reactor.api.event.resource;

public class UpscaleCheckHostMetadataRequest extends AbstractClusterScaleRequest {

    private final String primaryGatewayHostname;

    private final boolean singlePrimaryGateway;

    public UpscaleCheckHostMetadataRequest(Long stackId, String hostGroupName, String primaryGatewayHostname, boolean singlePrimaryGateway) {
        super(stackId, hostGroupName);
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
