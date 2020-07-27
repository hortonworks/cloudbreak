package com.sequenceiq.flow.core.helloworld;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;

import reactor.rx.Promise;

public abstract class HelloWorldSelectableEvent implements Selectable, Acceptable {

    private Long resourceId;

    private final Promise<AcceptResult> accepted = new Promise<>();

    public HelloWorldSelectableEvent(Long resourceId) {
        this.resourceId = resourceId;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public String selector() {
        return getClass().getSimpleName();
    }

}
