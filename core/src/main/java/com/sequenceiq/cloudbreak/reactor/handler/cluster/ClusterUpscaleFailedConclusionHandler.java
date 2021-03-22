package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_FAILED;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.conclusion.ConclusionChecker;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerFactory;
import com.sequenceiq.cloudbreak.conclusion.ConclusionCheckerType;
import com.sequenceiq.cloudbreak.conclusion.ConclusionResult;
import com.sequenceiq.cloudbreak.core.flow2.cluster.upscale.ClusterUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterUpscaleFailedConclusionRequest;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

import reactor.bus.Event;

@Component
public class ClusterUpscaleFailedConclusionHandler extends ExceptionCatcherEventHandler<ClusterUpscaleFailedConclusionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterUpscaleFailedConclusionHandler.class);

    @Inject
    private ConclusionCheckerFactory conclusionCheckerFactory;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterUpscaleFailedConclusionRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<ClusterUpscaleFailedConclusionRequest> event) {
        return new StackEvent(ClusterUpscaleEvent.FAIL_HANDLED_EVENT.event(), resourceId);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        ClusterUpscaleFailedConclusionRequest request = event.getData();
        LOGGER.info("Handle ClusterUpscaleFailedConclusionRequest, stackId: {}", request.getResourceId());
        try {
            ConclusionChecker conclusionChecker = conclusionCheckerFactory.getConclusionChecker(ConclusionCheckerType.DEFAULT);
            ConclusionResult conclusionResult = conclusionChecker.doCheck(request.getResourceId());
            flowMessageService.fireEventAndLog(request.getResourceId(), UPDATE_FAILED.name(), CLUSTER_SCALING_FAILED,
                    "added to", conclusionResult.getConclusions().toString());
        } catch (Exception e) {
            LOGGER.error("Error happened during conclusion check", e);
        }
        return new StackEvent(ClusterUpscaleEvent.FAIL_HANDLED_EVENT.event(), request.getResourceId());

    }

}
