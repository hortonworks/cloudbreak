package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RotateRdsCertificateOnProviderResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RotateRdsCertificateOnProviderResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
