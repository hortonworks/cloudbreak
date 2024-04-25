package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.rotaterdscert;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.rotaterdscert.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateCheckPrerequisitesResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class CheckRotateRdsCertificatePrerequisitesHandler extends ExceptionCatcherEventHandler<RotateRdsCertificateCheckPrerequisitesRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckRotateRdsCertificatePrerequisitesHandler.class);

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RotateRdsCertificateCheckPrerequisitesRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RotateRdsCertificateCheckPrerequisitesRequest> event) {
        return new RotateRdsCertificateFailedEvent(resourceId, e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<RotateRdsCertificateCheckPrerequisitesRequest> event) {
        RotateRdsCertificateCheckPrerequisitesRequest request = event.getData();
        Long stackId = request.getResourceId();
        rotateRdsCertificateService.checkPrerequisites(stackId);
        return new RotateRdsCertificateCheckPrerequisitesResult(stackId);
    }
}
