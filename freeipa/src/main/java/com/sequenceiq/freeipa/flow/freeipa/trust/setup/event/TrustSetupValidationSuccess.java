package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;

public class TrustSetupValidationSuccess extends StackEvent {
    private final TaskResults taskResults;

    @JsonCreator
    public TrustSetupValidationSuccess(@JsonProperty("resourceId") Long stackId, @JsonProperty("taskResults")TaskResults taskResults) {
        super(stackId);
        this.taskResults = taskResults;
    }

    public TaskResults getTaskResults() {
        return taskResults;
    }
}
