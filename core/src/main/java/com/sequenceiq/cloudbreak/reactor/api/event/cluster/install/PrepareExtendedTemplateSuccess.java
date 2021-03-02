package com.sequenceiq.cloudbreak.reactor.api.event.cluster.install;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PrepareExtendedTemplateSuccess extends StackEvent {
    public PrepareExtendedTemplateSuccess(Long stackId) {
        super(stackId);
    }
}
