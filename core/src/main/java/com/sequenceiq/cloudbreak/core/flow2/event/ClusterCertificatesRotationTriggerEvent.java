package com.sequenceiq.cloudbreak.core.flow2.event;

import java.util.Objects;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.CertificateRotationType;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

import reactor.rx.Promise;

public class ClusterCertificatesRotationTriggerEvent extends StackEvent {
    private final CertificateRotationType certificateRotationType;

    public ClusterCertificatesRotationTriggerEvent(String selector, Long stackId, CertificateRotationType certificateRotationType) {
        super(selector, stackId);
        this.certificateRotationType = certificateRotationType;
    }

    public ClusterCertificatesRotationTriggerEvent(String event, Long resourceId, Promise<AcceptResult> accepted,
            CertificateRotationType certificateRotationType) {
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
