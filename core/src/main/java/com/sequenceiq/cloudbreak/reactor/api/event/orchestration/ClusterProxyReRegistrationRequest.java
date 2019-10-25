package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.resource.AbstractClusterScaleRequest;

public class ClusterProxyReRegistrationRequest extends AbstractClusterScaleRequest {
    private String accountId;

    public ClusterProxyReRegistrationRequest(Long stackId, String accountId, String hostGroupName) {
        super(stackId, hostGroupName);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
