package com.sequenceiq.flow.reactor.api.event;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;

public record DelayFailedEvent(Long resourceId, Exception exception) implements Selectable {
    @Override
    public String selector() {
        return EventSelectorUtil.selector(getClass());
    }

    @Override
    public Long getResourceId() {
        return resourceId;
    }
}
