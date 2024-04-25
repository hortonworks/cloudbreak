package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RotateRdsCertificateTriggerRequest extends AbstractRotateRdsCertificateEvent {

    public RotateRdsCertificateTriggerRequest(Long stackId) {
        super(stackId);
    }

    @JsonCreator
    public RotateRdsCertificateTriggerRequest(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId) {
        super(selector, stackId);
    }
}
