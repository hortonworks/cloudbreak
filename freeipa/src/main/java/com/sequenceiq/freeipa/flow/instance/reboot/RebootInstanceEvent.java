package com.sequenceiq.freeipa.flow.instance.reboot;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.instance.InstanceEvent;

public class RebootInstanceEvent extends InstanceEvent {

    private final String operationId;

    @JsonCreator
    public RebootInstanceEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long resourceId,
            @JsonProperty("instanceIds") List<String> instanceIds,
            @JsonProperty("operationId") String operationId) {
        super(selector, resourceId, instanceIds);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public boolean equalsEvent(InstanceEvent other) {
        return isClassAndEqualsEvent(RebootInstanceEvent.class, other,
                event -> Objects.equals(operationId, event.operationId));
    }

}
