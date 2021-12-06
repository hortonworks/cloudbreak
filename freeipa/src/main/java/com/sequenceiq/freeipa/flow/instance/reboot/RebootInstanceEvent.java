package com.sequenceiq.freeipa.flow.instance.reboot;

import java.util.List;
import java.util.Objects;

import com.sequenceiq.freeipa.flow.instance.InstanceEvent;

public class RebootInstanceEvent extends InstanceEvent {

    private final String operationId;

    public RebootInstanceEvent(String selector, Long resourceId, List<String> instanceIds, String operationId) {
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
