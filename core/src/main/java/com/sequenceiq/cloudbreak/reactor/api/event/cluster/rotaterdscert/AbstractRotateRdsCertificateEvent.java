package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.RotateRdsCertificateType;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public abstract class AbstractRotateRdsCertificateEvent extends StackEvent implements RotateRdsCertificateBaseEvent {

    private RotateRdsCertificateType rotateRdsCertificateType;

    public AbstractRotateRdsCertificateEvent(Long stackId, RotateRdsCertificateType rotateRdsCertificateType) {
        super(stackId);
        this.rotateRdsCertificateType = rotateRdsCertificateType;
    }

    public AbstractRotateRdsCertificateEvent(String selector, Long stackId, RotateRdsCertificateType rotateRdsCertificateType) {
        super(selector, stackId);
        this.rotateRdsCertificateType = rotateRdsCertificateType;
    }

    @Override
    public RotateRdsCertificateType getRotateRdsCertificateType() {
        return this.rotateRdsCertificateType;
    }

}
