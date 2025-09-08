package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;

public class TrustSetupFailureEvent extends StackFailureEvent {
    private final TaskResults taskResults;

    public TrustSetupFailureEvent(Long stackId, Exception exception) {
        this(stackId, exception, new TaskResults());
    }

    @JsonCreator
    public TrustSetupFailureEvent(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception,
            @JsonProperty("taskResults") TaskResults taskResults) {
        super(stackId, exception);
        this.taskResults = taskResults;
    }

    public TaskResults getTaskResults() {
        return taskResults;
    }
}
