package com.sequenceiq.freeipa.flow.instance.reboot;

import java.util.List;

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
}
