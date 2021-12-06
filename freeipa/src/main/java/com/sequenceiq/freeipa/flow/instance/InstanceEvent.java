package com.sequenceiq.freeipa.flow.instance;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.IdempotentEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

import reactor.rx.Promise;

public class InstanceEvent implements IdempotentEvent<InstanceEvent> {

    private final String selector;

    private final Long resourceId;

    private final Promise<AcceptResult> accepted;

    private final List<String> instanceIds;

    public InstanceEvent(Long resourceId) {
        this(null, resourceId, null);
    }

    public InstanceEvent(Long resourceId, List<String> instanceIds) {
        this(null, resourceId, instanceIds);
    }

    public InstanceEvent(String selector, Long resourceId, List<String> instanceIds) {
        this.selector = selector;
        this.resourceId = resourceId;
        this.instanceIds = instanceIds;
        accepted = new Promise<>();
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }

    public List<String> getInstanceIds() {
        return instanceIds;
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
    public boolean equalsEvent(InstanceEvent other) {
        return isClassAndEqualsEvent(InstanceEvent.class, other);
    }

    protected <T extends InstanceEvent> boolean isClassAndEqualsEvent(Class<T> clazz, InstanceEvent other) {
        return isClassAndEqualsEvent(clazz, other, instanceEvent -> true);
    }

    protected <T extends InstanceEvent> boolean isClassAndEqualsEvent(Class<T> clazz, InstanceEvent other, Predicate<T> equalsSubclass) {
        if (!clazz.equals(getClass())) {
            return false;
        }
        return Objects.equals(selector, other.selector)
                && Objects.equals(resourceId, other.resourceId)
                && Objects.equals(instanceIds, other.instanceIds)
                && equalsSubclass.test((T) other);
    }

    @Override
    public String toString() {
        return "InstanceEvent{" +
                "selector='" + selector + '\'' +
                ", resourceId=" + resourceId +
                ", accepted=" + accepted +
                ", instanceIds=" + instanceIds +
                '}';
    }
}
