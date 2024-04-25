package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RotateRdsCertificateCheckPrerequisitesResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RotateRdsCertificateCheckPrerequisitesResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
