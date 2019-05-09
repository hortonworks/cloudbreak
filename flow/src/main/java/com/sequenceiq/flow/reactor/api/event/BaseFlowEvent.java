package com.sequenceiq.flow.reactor.api.event;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;

import reactor.rx.Promise;

public class BaseFlowEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long resourceId;

    private final Promise<Boolean> accepted;

    public BaseFlowEvent(String selector, Long resourceId) {
        this(selector, resourceId, new Promise<>());
    }

    public BaseFlowEvent(String selector, Long resourceId, Promise<Boolean> accepted) {
        this.selector = selector;
        this.resourceId = resourceId;
        this.accepted = accepted;
    }

    @Override
    public Long getStackId() {
        return resourceId;
    }

    @Override
    public String selector() {
        return selector;
    }

    @Override
    public Promise<Boolean> accepted() {
        return accepted;
    }
}
