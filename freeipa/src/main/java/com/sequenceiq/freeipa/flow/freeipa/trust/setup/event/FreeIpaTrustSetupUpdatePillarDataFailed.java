package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.freeipa.common.FailureType;

public class FreeIpaTrustSetupUpdatePillarDataFailed extends FreeIpaTrustSetupFailureEvent {

    @JsonCreator
    public FreeIpaTrustSetupUpdatePillarDataFailed(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception exception) {
        super(stackId, exception, FailureType.ERROR);
    }
}
