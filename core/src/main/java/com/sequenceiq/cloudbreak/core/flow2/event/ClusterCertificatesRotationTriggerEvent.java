package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.json.JsonIgnoreDeserialization;
import com.sequenceiq.cloudbreak.eventbus.Promise;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public class ClusterCertificatesRotationTriggerEvent extends StackEvent {
    private final CertificateRotationType certificateRotationType;

    public ClusterCertificatesRotationTriggerEvent(String selector, Long stackId, CertificateRotationType certificateRotationType) {
        super(selector, stackId);
        this.certificateRotationType = certificateRotationType;
    }

    @JsonCreator
    public ClusterCertificatesRotationTriggerEvent(
            @JsonProperty("selector") String event,
            @JsonProperty("resourceId") Long resourceId,
            @JsonIgnoreDeserialization @JsonProperty("accepted") Promise<AcceptResult> accepted,
            @JsonProperty("certificateRotationType") CertificateRotationType certificateRotationType) {
        super(event, resourceId, accepted);
        this.certificateRotationType = certificateRotationType;
    }

    public CertificateRotationType getCertificateRotationType() {
        return certificateRotationType;
    }

    @Override
    public boolean equalsEvent(StackEvent other) {
        return isClassAndEqualsEvent(ClusterCertificatesRotationTriggerEvent.class, other,
                event -> Objects.equals(certificateRotationType, event.certificateRotationType));
    }
}
