package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.rotaterdscert;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateOnProviderResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RotateRdsCertificateOnProviderHandler extends ExceptionCatcherEventHandler<RotateRdsCertificateOnProviderRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RotateRdsCertificateOnProviderHandler.class);

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RotateRdsCertificateOnProviderRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RotateRdsCertificateOnProviderRequest> event) {
        return new RotateRdsCertificateFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<RotateRdsCertificateOnProviderRequest> event) {
        RotateRdsCertificateOnProviderRequest request = event.getData();
        Long stackId = request.getResourceId();
        rotateRdsCertificateService.rotateOnProvider(stackId);
        return new RotateRdsCertificateOnProviderResult(stackId);
    }
}
