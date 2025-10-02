package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.service.freeipa.trust.operation.TaskResults;

public class FreeIpaTrustSetupValidationFailed extends FreeIpaTrustSetupFailureEvent {
    public FreeIpaTrustSetupValidationFailed(Long stackId, Exception exception) {
        super(stackId, exception);
    }

    public FreeIpaTrustSetupValidationFailed(Long stackId, TaskResults taskResults) {
        super(stackId, null, taskResults);
    }

    @JsonCreator
    public FreeIpaTrustSetupValidationFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("taskResults") TaskResults taskResults) {
        super(stackId, exception, taskResults);
    }
}
