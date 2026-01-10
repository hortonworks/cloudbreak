package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.CREATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_CREATE_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.ClusterCreationEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterCreationFailedRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterCreationFailedHandler extends ExceptionCatcherEventHandler<ClusterCreationFailedRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterCreationFailedHandler.class);

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterCreationFailedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterCreationFailedRequest> event) {
        return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterCreationFailedRequest> event) {
        ClusterCreationFailedRequest request = event.getData();
        LOGGER.info("Handle ClusterCreationFailedRequest, stackId: {}", request.getResourceId());
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), CREATE_FAILED.name(), CLUSTER_CREATE_FAILED, request.getConclusionCheckerType());
        return new StackEvent(ClusterCreationEvent.CLUSTER_CREATION_FAILURE_HANDLED_EVENT.event(), request.getResourceId());
    }
}
