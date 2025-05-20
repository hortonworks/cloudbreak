package com.sequenceiq.freeipa.flow.freeipa.enableselinux.event;

import java.util.Objects;
import java.util.StringJoiner;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class FreeIpaModifySeLinuxEvent extends StackEvent {

    private final String operationId;

    private final SeLinux seLinuxMode;

    @JsonCreator
    public FreeIpaModifySeLinuxEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("operationId") String operationId,
            @JsonProperty("seLinuxMode") SeLinux seLinuxMode) {
        super(selector, resourceId);
        this.operationId = operationId;
        this.seLinuxMode = seLinuxMode;
    }

    public String getOperationId() {
        return operationId;
    }

    public SeLinux getSeLinuxMode() {
        return seLinuxMode;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(FreeIpaModifySeLinuxEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FreeIpaModifySeLinuxEvent.class.getSimpleName() + "[", "]")
                .add("operationId=" + operationId)
                .add("seLinuxMode=" + seLinuxMode)
                .toString();
    }
}
