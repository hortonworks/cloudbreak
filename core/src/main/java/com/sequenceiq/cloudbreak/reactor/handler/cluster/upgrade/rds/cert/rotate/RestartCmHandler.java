package com.sequenceiq.cloudbreak.reactor.handler.cluster.upgrade.rds.cert.rotate;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.cluster.ClusterManagerRestartService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RestartCmResult;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.rotaterdscert.RotateRdsCertificateFailedEvent;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class RestartCmHandler extends ExceptionCatcherEventHandler<RestartCmRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RestartCmHandler.class);

    @Inject
    private ClusterManagerRestartService clusterManagerRestartService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RestartCmRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RestartCmRequest> event) {
        return new RotateRdsCertificateFailedEvent(resourceId, event.getData().getRotateRdsCertificateType(), e);
    }

    @Override
    public Selectable doAccept(HandlerEvent<RestartCmRequest> event) {
        RestartCmRequest request = event.getData();
        LOGGER.debug("Restarting Cloudera Manager {}", request);
        Long stackId = request.getResourceId();
        clusterManagerRestartService.restartClouderaManager(stackId);
        return new RestartCmResult(stackId, request.getRotateRdsCertificateType());
    }
}
