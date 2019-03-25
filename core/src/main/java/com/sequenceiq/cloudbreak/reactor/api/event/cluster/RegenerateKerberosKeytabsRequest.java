package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class RegenerateKerberosKeytabsRequest extends AbstractClusterScaleRequest {

    private String hostname;

    public RegenerateKerberosKeytabsRequest(Long stackId, String hostGroupName, String hostName) {
        super(stackId, hostGroupName);
        hostname = hostName;
    }

    public String getHostname() {
        return hostname;
    }
}
