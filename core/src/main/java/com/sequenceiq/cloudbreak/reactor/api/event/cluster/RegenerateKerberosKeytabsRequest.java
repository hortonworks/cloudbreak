package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import java.util.Set;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class RegenerateKerberosKeytabsRequest extends AbstractClusterScaleRequest {

    private String hostname;

    public RegenerateKerberosKeytabsRequest(Long stackId, Set<String> hostGroups, String hostName) {
        super(stackId, hostGroups);
        hostname = hostName;
    }

    public String getHostname() {
        return hostname;
    }
}
