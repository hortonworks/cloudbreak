package com.sequenceiq.cloudbreak.core.flow2.validate.kerberosconfig.event;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class PollBindUserCreationEvent extends StackEvent {

    private final String operationId;

    public PollBindUserCreationEvent(Long stackId, String operationId) {
        super(stackId);
        this.operationId = operationId;
    }

    public PollBindUserCreationEvent(String selector, Long stackId, String operationId) {
        super(selector, stackId);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }
}
