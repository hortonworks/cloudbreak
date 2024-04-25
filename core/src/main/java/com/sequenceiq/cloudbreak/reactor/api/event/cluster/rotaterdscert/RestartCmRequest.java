package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestartCmRequest extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RestartCmRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
