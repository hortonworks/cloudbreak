package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ClusterUpscaleFailedConclusionHandler extends ExceptionCatcherEventHandler<ClusterUpscaleFailedConclusionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFailedConclusionHandler.class);

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpscaleFailedConclusionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpscaleFailedConclusionRequest> event) {
        return new StackEvent(ClusterUpscaleEvent.FAIL_HANDLED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<ClusterUpscaleFailedConclusionRequest> event) {
        ClusterUpscaleFailedConclusionRequest request = event.getData();
        LOGGER.info("Handle ClusterUpscaleFailedConclusionRequest, stackId: {}", request.getResourceId());
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED, ConclusionCheckerType.DEFAULT,
                "added to");
        return new StackEvent(ClusterUpscaleEvent.FAIL_HANDLED_EVENT.event(), request.getResourceId());
    }

}
