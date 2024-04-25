package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateLatestRdsCertificateResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public UpdateLatestRdsCertificateResult(
            @JsonProperty("resourceId") Long stackId) {
        super(stackId);
    }
}
