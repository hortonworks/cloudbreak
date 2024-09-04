package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType;

public class RotateRdsCertificateTriggerRequest extends AbstractRotateRdsCertificateEvent {

    public RotateRdsCertificateTriggerRequest(Long stackId, RotateRdsCertificateType rotateRdsCertificateType) {
        super(stackId, rotateRdsCertificateType);
    }

    @JsonCreator
    public RotateRdsCertificateTriggerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("rotateRdsCertificateType") RotateRdsCertificateType rotateRdsCertificateType) {
        super(selector, stackId, rotateRdsCertificateType);
    }
}
