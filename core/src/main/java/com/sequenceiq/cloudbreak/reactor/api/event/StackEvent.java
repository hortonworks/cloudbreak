package com.sequenceiq.cloudbreak.reactor.api.event;

import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class StackEvent implements IdempotentEvent<StackEvent> {
    private final String selector;

    private final Long stackId;

    private final Promise<AcceptResult> accepted;

    public StackEvent(Long stackId) {
        this(null, stackId);
    }

    public StackEvent(String selector, Long stackId) {
        this.selector = selector;
        this.stackId = stackId;
        accepted = new Promise<>();
    }

    public StackEvent(String selector, Long stackId, Promise<AcceptResult> accepted) {
        this.selector = selector;
        this.stackId = stackId;
        this.accepted = accepted;
    }

    @Override
    public Long getResourceId() {
        return stackId;
    }

    @Override
    public String selector() {
        return StringUtils.isNotEmpty(selector) ? selector : EventSelectorUtil.selector(getClass());
    }

    @Override
    public Promise<AcceptResult> accepted() {
        return accepted;
    }

    @Override
    public String toString() {
        return "StackEvent{" +
                "selector='" + selector() + '\'' +
                ", stackId=" + stackId +
                ", accepted=" + accepted +
                '}';
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(StackEvent.class, other);
    }

    protected <T extends StackEvent> boolean isClassAndEqualsEvent(Class<T> clazz, StackEvent other) {
        return isClassAndEqualsEvent(clazz, other, stackEvent -> true);
    }

    protected <T extends StackEvent> boolean isClassAndEqualsEvent(Class<T> clazz, StackEvent other, Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(stackId, other.stackId)
                && equalsSubclass.test((T) other);
    }
}
