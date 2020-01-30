package com.sequenceiq.redbeams.flow.redbeams.common;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class RedbeamsEvent implements Selectable, Acceptable {
    private final String selector;

    private final Long resourceId;

    private final Promise<AcceptResult> accepted;

    private final boolean forced;

    public RedbeamsEvent(Long resourceId) {
        this(null, resourceId);
    }

    public RedbeamsEvent(Long resourceId, boolean forced) {
        this(null, resourceId, forced);
    }

    public RedbeamsEvent(String selector, Long resourceId) {
        this(selector, resourceId, new Promise<>(), false);
    }

    public RedbeamsEvent(String selector, Long resourceId, boolean forced) {
        this(selector, resourceId, new Promise<>(), forced);
    }

    public RedbeamsEvent(String selector, Long resourceId, Promise<AcceptResult> accepted, boolean forced) {
        this.selector = selector;
        this.resourceId = resourceId;
        this.accepted = accepted;
        this.forced = forced;
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
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    public boolean isForced() {
        return forced;
    }

    public String toString() {
        return selector;
    }
}
