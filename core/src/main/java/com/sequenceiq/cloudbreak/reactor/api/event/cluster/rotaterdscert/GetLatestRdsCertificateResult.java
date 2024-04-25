package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GetLatestRdsCertificateResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public GetLatestRdsCertificateResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
