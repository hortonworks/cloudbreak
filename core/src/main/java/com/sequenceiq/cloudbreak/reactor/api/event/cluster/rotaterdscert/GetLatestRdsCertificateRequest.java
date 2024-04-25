package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetLatestRdsCertificateRequest extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public GetLatestRdsCertificateRequest(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
