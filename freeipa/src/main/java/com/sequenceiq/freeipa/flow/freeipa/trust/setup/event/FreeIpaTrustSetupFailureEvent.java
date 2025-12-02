package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.flow.stack.StackFailureEvent;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;

public class FreeIpaTrustSetupFailureEvent extends StackFailureEvent {
    private final TaskResults taskResults;

    public FreeIpaTrustSetupFailureEvent(Long stackId, Exception exception, FailureType failureType) {
        this(stackId, exception, new TaskResults(), failureType);
    }

    @JsonCreator
    public FreeIpaTrustSetupFailureEvent(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("taskResults") TaskResults taskResults,
            @JsonProperty("failureType") FailureType failureType) {
        super(stackId, exception, failureType);
        this.taskResults = taskResults;
    }

    public TaskResults getTaskResults() {
        return taskResults;
    }
}
