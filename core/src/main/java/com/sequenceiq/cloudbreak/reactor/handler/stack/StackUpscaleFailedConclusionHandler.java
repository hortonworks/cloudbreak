package com.sequenceiq.cloudbreak.reactor.handler.stack;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerService;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.upscale.StackUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.StackUpscaleFailedConclusionRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class StackUpscaleFailedConclusionHandler extends ExceptionCatcherEventHandler<StackUpscaleFailedConclusionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackUpscaleFailedConclusionHandler.class);

    @Inject
    private ConclusionCheckerService conclusionCheckerService;

    @Inject
    private StackUpscaleService stackUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StackUpscaleFailedConclusionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StackUpscaleFailedConclusionRequest> event) {
        return new StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StackUpscaleFailedConclusionRequest> event) {
        StackUpscaleFailedConclusionRequest request = event.getData();
        LOGGER.info("Handle StackUpscaleFailedConclusionRequest, stackId: {}", request.getResourceId());
        conclusionCheckerService.runConclusionChecker(request.getResourceId(), UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED,
                ConclusionCheckerType.STACK_PROVISION, "added to");
        stackUpscaleService.handleStackUpscaleFailure(request.isRepair(), request.getHostgroupWithHostnames(), request.getException(),
                request.getResourceId(), request.getHostGroupWithAdjustment());
        return new StackEvent(StackUpscaleEvent.UPSCALE_FAIL_HANDLED_EVENT.event(), request.getResourceId());
    }
}
