package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RollingRestartServicesRequest extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RollingRestartServicesRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
