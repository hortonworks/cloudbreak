package com.sequenceiq.flow.reactor.api.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

public record DelayEvent(Long resourceId, Selectable successEvent, long delayInSec, boolean sendSuccessInCaseOfFailure) implements Selectable {

    public DelayEvent {
        Objects.requireNonNull(successEvent);
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(getClass());
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }
}
