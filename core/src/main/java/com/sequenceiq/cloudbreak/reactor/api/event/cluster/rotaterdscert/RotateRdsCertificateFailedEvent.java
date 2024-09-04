package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;

public class RotateRdsCertificateFailedEvent extends StackFailureEvent implements RotateRdsCertificateBaseEvent {

    private final RotateRdsCertificateType rotateRdsCertificateType;

    @JsonCreator
    public RotateRdsCertificateFailedEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("rotateRdsCertificateType") RotateRdsCertificateType rotateRdsCertificateType,
            @JsonProperty("exception") Exception ex) {
        super(selector, stackId, ex);
        this.rotateRdsCertificateType = rotateRdsCertificateType;
    }

    public RotateRdsCertificateFailedEvent(
            Long stackId,
            RotateRdsCertificateType rotateRdsCertificateType,
            Exception ex) {
        super(stackId, ex);
        this.rotateRdsCertificateType = rotateRdsCertificateType;
    }

    @Override
    public String toString() {
        return "RotateRdsCertificateFailedEven{" +
                "} " + super.toString();
    }

    @Override
    public RotateRdsCertificateType getRotateRdsCertificateType() {
        return this.rotateRdsCertificateType;
    }
}
