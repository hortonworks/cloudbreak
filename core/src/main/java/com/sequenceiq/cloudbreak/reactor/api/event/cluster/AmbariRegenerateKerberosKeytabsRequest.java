package com.sequenceiq.cloudbreak.reactor.api.event.cluster;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class AmbariRegenerateKerberosKeytabsRequest extends AbstractClusterScaleRequest {

    private String hostname;

    public AmbariRegenerateKerberosKeytabsRequest(Long stackId, String hostGroupName, String hostName) {
        super(stackId, hostGroupName);
        this.hostname = hostName;
    }

    public String getHostname() {
        return hostname;
    }
}
