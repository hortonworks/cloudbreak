package com.sequenceiq.cloudbreak.reactor.api.event.cluster.certrotate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCMCARotationRequest extends StackEvent {

    private final CertificateRotationType certificateRotationType;

    @JsonCreator
    public ClusterCMCARotationRequest(
            @JsonProperty("resourceId") Long stackId,
            @JsonProperty("certificateRotationType") CertificateRotationType certificateRotationType) {
        super(stackId);
        this.certificateRotationType = certificateRotationType;
    }

    public CertificateRotationType getCertificateRotationType() {
        return certificateRotationType;
    }

    @Override
    public String toString() {
        return "ClusterCMCARotationRequest{" +
                "certificateRotationType=" + certificateRotationType +
                '}';
    }
}
