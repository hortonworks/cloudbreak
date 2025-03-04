package com.sequenceiq.freeipa.flow.freeipa.enableselinux.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaEnableSeLinuxHandlerEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public FreeIpaEnableSeLinuxHandlerEvent(
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId) {
        super(EventSelectorUtil.selector(FreeIpaEnableSeLinuxHandlerEvent.class), resourceId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(FreeIpaEnableSeLinuxHandlerEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIpaEnableSeLinuxHandlerEvent.class.getSimpleName() + "[", "]")
                .add("operationId=" + operationId)
                .toString();
    }
}
