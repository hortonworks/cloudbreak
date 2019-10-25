package com.sequenceiq.cloudbreak.reactor.api.event.orchestration;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterProxyRegistrationRequest extends StackEvent {
    private final String accountId;

    public ClusterProxyRegistrationRequest(Long stackId, String accountId) {
        super(stackId);
        this.accountId = accountId;
    }

    public String getAccountId() {
        return accountId;
    }
}
