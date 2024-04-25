package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RotateRdsCertificateOnProviderRequest extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RotateRdsCertificateOnProviderRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
