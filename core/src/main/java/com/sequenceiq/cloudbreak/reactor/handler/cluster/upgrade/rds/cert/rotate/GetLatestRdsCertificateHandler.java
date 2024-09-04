package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.rds.cert.rotate.RotateRdsCertificateService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.GetLatestRdsCertificateResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class GetLatestRdsCertificateHandler extends ExceptionCatcherEventHandler<GetLatestRdsCertificateRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetLatestRdsCertificateHandler.class);

    @Inject
    private RotateRdsCertificateService rotateRdsCertificateService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(GetLatestRdsCertificateRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<GetLatestRdsCertificateRequest> event) {
        return new RotateRdsCertificateFailedEvent(resourceId, event.getData().getRotateRdsCertificateType(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<GetLatestRdsCertificateRequest> event) {
        GetLatestRdsCertificateRequest request = event.getData();
        Long stackId = request.getResourceId();
        rotateRdsCertificateService.getLatestRdsCertificate(stackId);
        return new GetLatestRdsCertificateResult(stackId, request.getRotateRdsCertificateType());
    }
}
