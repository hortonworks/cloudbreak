package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class TrustSetupEvent extends StackEvent {

    private final String operationId;

    @JsonCreator
    public TrustSetupEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("operationId") String operationId) {
        super(stackId);
        this.operationId = operationId;
    }

    public TrustSetupEvent(Long stackId, String operationId, Promise<AcceptResult> accepted) {
        super(null, stackId, accepted);
        this.operationId = operationId;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "TrustSetupEvent{" +
                "operationId='" + operationId + '\'' +
                '}';
    }
}
