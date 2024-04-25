package com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert;

import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;

public abstract class AbstractRotateRdsCertificateEvent extends StackEvent implements RotateRdsCertificateBaseEvent {

    public AbstractRotateRdsCertificateEvent(Long stackId) {
        super(stackId);
    }

    public AbstractRotateRdsCertificateEvent(String selector, Long stackId) {
        super(selector, stackId);
    }

}
