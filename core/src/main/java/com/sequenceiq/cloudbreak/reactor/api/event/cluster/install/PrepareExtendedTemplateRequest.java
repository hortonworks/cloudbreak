package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PrepareExtendedTemplateRequest extends StackEvent {
    public PrepareExtendedTemplateRequest(Long stackId) {
        super(stackId);
    }
}
