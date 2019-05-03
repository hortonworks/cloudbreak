package com.sequenceiq.freeipa.flow.stack;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.Acceptable;
import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.freeipa.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class StackEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long stackId;

    private final Promise<Boolean> accepted;

    public StackEvent(Long stackId) {
        this(null, stackId);
    }

    public StackEvent(String selector, Long stackId) {
        this.selector = selector;
        this.stackId = stackId;
        accepted = new Promise<>();
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
