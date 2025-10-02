package com.sequenceiq.freeipa.flow.freeipa.trust.setup.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FreeIpaTrustSetupPrepareServerFailed extends FreeIpaTrustSetupFailureEvent {

    @JsonCreator
    public FreeIpaTrustSetupPrepareServerFailed(@JsonProperty("resourceId") Long stackId, @JsonProperty("exception") Exception exception) {
        super(stackId, exception);
    }
}
