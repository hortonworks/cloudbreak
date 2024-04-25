package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RestartCmResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RestartCmResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
