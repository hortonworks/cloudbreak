package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;

public class FreeIpaTrustSetupValidationFailed extends FreeIpaTrustSetupFailureEvent {
    public FreeIpaTrustSetupValidationFailed(Long stackId, Exception exception, FailureType failureType) {
        super(stackId, exception, failureType);
    }

    public FreeIpaTrustSetupValidationFailed(Long stackId, TaskResults taskResults, FailureType failureType) {
        super(stackId, null, taskResults, failureType);
    }

    @JsonCreator
    public FreeIpaTrustSetupValidationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("taskResults") TaskResults taskResults,
            @JsonProperty("failureType") FailureType failureType) {
        super(stackId, exception, taskResults, failureType);
    }
}
