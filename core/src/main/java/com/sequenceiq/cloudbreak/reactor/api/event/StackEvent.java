package com.sequenceiq.cloudbreak.reactor.api.event;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;

import reactor.rx.Promise;

public class StackEvent implements Selectable, Acceptable {
    private String selector;

    private Long stackId;

    private Promise<Boolean> accepted;

    public StackEvent(Long stackId) {
        this(null, stackId);
    }

    public StackEvent(String selector, Long stackId) {
        this.selector = selector;
        this.stackId = stackId;
        this.accepted = new Promise<>();
    }

    public StackEvent(String selector, Long stackId, Promise<Boolean> accepted) {
        this.selector = selector;
        this.stackId = stackId;
        this.accepted = accepted;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    @Override
    public Promise<Boolean> accepted() {
        return accepted;
    }
}
