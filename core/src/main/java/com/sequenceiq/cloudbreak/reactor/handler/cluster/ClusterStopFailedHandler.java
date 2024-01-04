package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.STOP_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_STOP_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stop.ClusterStopEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStopFailedRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterStopFailedHandler extends ExceptionCatcherEventHandler<ClusterStopFailedRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStopFailedHandler.class);

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStopFailedRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterStopFailedRequest> event) {
        return new StackEvent(ClusterStopEvent.FINALIZED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterStopFailedRequest> event) {
        ClusterStopFailedRequest request = event.getData();
        LOGGER.info("Handle ClusterStopFailedRequest, stackId: {}", request.getResourceId());
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), STOP_FAILED.name(), CLUSTER_STOP_FAILED, ConclusionCheckerType.DEFAULT);
        return new StackEvent(ClusterStopEvent.FINALIZED_EVENT.event(), request.getResourceId());
    }
}
