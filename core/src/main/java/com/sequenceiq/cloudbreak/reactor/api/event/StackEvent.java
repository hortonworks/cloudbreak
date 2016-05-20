package com.sequenceiq.cloudbreak.reactor.api.event;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;

public class StackEvent implements Selectable {
    private String selector;
    private Long stackId;

    public StackEvent(Long stackId) {
        this(null, stackId);
    }

    public StackEvent(String selector, Long stackId) {
        this.selector = selector;
        this.stackId = stackId;
    }

    @Override
    public Long getStackId() {
        return stackId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }
}
