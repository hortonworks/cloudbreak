package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class RegenerateKerberosKeytabsRequest extends AbstractClusterScaleRequest {

    private final String hostname;

    @JsonCreator
    public RegenerateKerberosKeytabsRequest(
            @JsonProperty("stackId") Long stackId,
            @JsonProperty("hostGroupNames") Set<String> hostGroups,
            @JsonProperty("hostname") String hostname) {
        super(stackId, hostGroups);
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }
}
