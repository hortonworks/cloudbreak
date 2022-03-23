package com.sequenceiq.cloudbreak.reactor.api.event.stack;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class UpdateDomainDnsResolverRequest extends StackEvent {

    public UpdateDomainDnsResolverRequest(Long stackId) {
        super(stackId);
    }
}
