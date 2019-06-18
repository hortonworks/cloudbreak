package com.sequenceiq.redbeams.flow.redbeams.common;

import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

import org.apache.commons.lang3.StringUtils;

import reactor.rx.Promise;

public class RedbeamsEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long resourceId;

    private final Promise<Boolean> accepted;

    public RedbeamsEvent(Long resourceId) {
        this(null, resourceId);
    }

    public RedbeamsEvent(String selector, Long resourceId) {
        this(selector, resourceId, new Promise<>());
    }

    public RedbeamsEvent(String selector, Long resourceId, Promise<Boolean> accepted) {
        this.selector = selector;
        this.resourceId = resourceId;
        this.accepted = accepted;
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    @Override
    public Promise<Boolean> accepted() {
        return accepted;
    }

    public String toString() {
        return selector;
    }
}
