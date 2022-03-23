package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateDomainDnsResolverResult extends StackEvent {

    public UpdateDomainDnsResolverResult(Long stackId) {
        super(stackId);
    }
}
