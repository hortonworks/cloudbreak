package com.sequenceiq.freeipa.flow.freeipa.trust.cancel.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FreeIpaTrustCancelConfigurationFailed extends FreeIpaTrustCancelFailureEvent {

    @JsonCreator
    public FreeIpaTrustCancelConfigurationFailed(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
