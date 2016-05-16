package com.sequenceiq.cloudbreak.reactor.api.event;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;

public class StackPayload implements Selectable {
    private String selector;
    private Long stackId;

    public StackPayload(String selector, Long stackId) {
        this.selector = selector;
        this.stackId = stackId;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public String selector() {
        return selector;
    }
}
