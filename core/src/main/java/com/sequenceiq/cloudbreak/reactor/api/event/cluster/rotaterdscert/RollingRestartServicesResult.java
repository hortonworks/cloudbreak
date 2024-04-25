package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RollingRestartServicesResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RollingRestartServicesResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
