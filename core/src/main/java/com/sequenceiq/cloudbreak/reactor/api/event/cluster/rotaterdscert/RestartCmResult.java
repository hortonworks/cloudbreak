package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType;

public class RestartCmResult extends AbstractRotateRdsCertificateEvent {

    @JsonCreator
    public RestartCmResult(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("rotateRdsCertificateType") RotateRdsCertificateType rotateRdsCertificateType) {
        super(stackId, rotateRdsCertificateType);
    }
}
