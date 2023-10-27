package com.sequenceiq.redbeams.flow.redbeams.common;

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class RedbeamsEvent implements IdempotentEvent<RedbeamsEvent> {

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

    @JsonCreator
    public RedbeamsEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("forced") boolean forced) {

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

    @Override
    public String toString() {
        return "RedbeamsEvent{" +
                "selector='" + selector() + '\'' +
                ", resourceId=" + resourceId +
                ", accepted=" + accepted +
                ", forced=" + forced +
                '}';
    }

    @Override
    public boolean equalsEvent(RedbeamsEvent other) {
        return isClassAndEqualsEvent(RedbeamsEvent.class, other);
    }

    protected <T extends RedbeamsEvent> boolean isClassAndEqualsEvent(Class<T> clazz, RedbeamsEvent other) {
        return isClassAndEqualsEvent(clazz, other, redbeamsEvent -> true);
    }

    protected <T extends RedbeamsEvent> boolean isClassAndEqualsEvent(Class<T> clazz, RedbeamsEvent other, Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(resourceId, other.resourceId)
                && equalsSubclass.test((T) other);
    }
}
