package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RotateRdsCertificateCheckPrerequisitesRequest extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RotateRdsCertificateCheckPrerequisitesRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
