package com.sequenceiq.freeipa.flow.freeipa.enableselinux.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaEnableSeLinuxEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public FreeIpaEnableSeLinuxEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId) {
        super(selector, resourceId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(FreeIpaEnableSeLinuxEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIpaEnableSeLinuxEvent.class.getSimpleName() + "[", "]")
                .add("operationId=" + operationId)
                .toString();
    }
}
