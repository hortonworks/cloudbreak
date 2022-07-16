package com.sequenceiq.cloudbreak.reactor.api.event.resource;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpscaleCheckHostMetadataRequest extends AbstractClusterScaleRequest {

    private final String primaryGatewayHostname;

    private final boolean singlePrimaryGateway;

    @JsonCreator
    public UpscaleCheckHostMetadataRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("primaryGatewayHostname") String primaryGatewayHostname,
            @JsonProperty("singlePrimaryGateway") boolean singlePrimaryGateway) {
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
