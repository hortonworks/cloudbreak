package com.sequenceiq.freeipa.flow.freeipa.trust.setupfinish.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;

public class FreeIpaTrustSetupFinishAddTrustFailed extends FreeIpaTrustSetupFinishFailureEvent {

    @JsonCreator
    public FreeIpaTrustSetupFinishAddTrustFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception,
            @JsonProperty("failureType") FailureType failureType) {
        super(stackId, exception, failureType);
    }
}
