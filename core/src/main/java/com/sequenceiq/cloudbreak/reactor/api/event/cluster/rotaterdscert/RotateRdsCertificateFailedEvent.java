package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RotateRdsCertificateFailedEvent extends StackFailureEvent implements RotateRdsCertificateBaseEvent {

    @JsonCreator
    public RotateRdsCertificateFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("exception") Exception ex) {
        super(selector, stackId, ex);
    }

    public RotateRdsCertificateFailedEvent(
            Long stackId,
            Exception ex) {
        super(stackId, ex);
    }

    @Override
    public String toString() {
        return "RotateRdsCertificateFailedEven{" +
                "} " + super.toString();
    }
}
