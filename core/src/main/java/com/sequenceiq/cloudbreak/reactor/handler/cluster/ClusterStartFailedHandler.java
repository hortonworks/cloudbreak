package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.START_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_START_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.start.ClusterStartEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartFailedRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterStartFailedHandler extends ExceptionCatcherEventHandler<ClusterStartFailedRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartFailedHandler.class);

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartFailedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterStartFailedRequest> event) {
        return new StackEvent(ClusterStartEvent.FAIL_HANDLED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterStartFailedRequest> event) {
        ClusterStartFailedRequest request = event.getData();
        LOGGER.info("Handle ClusterStartFailedRequest, stackId: {}", request.getResourceId());
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), START_FAILED.name(), CLUSTER_START_FAILED, ConclusionCheckerType.DEFAULT);
        return new StackEvent(ClusterStartEvent.FAIL_HANDLED_EVENT.event(), request.getResourceId());
    }
}
