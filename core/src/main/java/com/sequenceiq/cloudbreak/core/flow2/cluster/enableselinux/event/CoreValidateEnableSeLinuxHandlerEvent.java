package com.sequenceiq.cloudbreak.core.flow2.cluster.enableselinux.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;

public class CoreValidateEnableSeLinuxHandlerEvent extends StackEvent {

    @JsonCreator
    public CoreValidateEnableSeLinuxHandlerEvent(@JsonProperty("resourceId") Long resourceId) {
        super(EventSelectorUtil.selector(CoreValidateEnableSeLinuxHandlerEvent.class), resourceId);
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(CoreValidateEnableSeLinuxHandlerEvent.class, other,
                event -> Objects.equals(getResourceId(), event.getResourceId()));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CoreValidateEnableSeLinuxHandlerEvent.class.getSimpleName() + "[", "]")
                .add("selector=" + getSelector())
                .add("resourceId=" + getResourceId())
                .toString();
    }
}
