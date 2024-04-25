package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateLatestRdsCertificateRequest extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public UpdateLatestRdsCertificateRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
