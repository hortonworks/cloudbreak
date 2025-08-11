package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateProcessor;
import com.sequenceiq.cloudbreak.core.cluster.ClusterStartHandlerService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.ClusterStartResult;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class ClusterStartHandler implements EventHandler<ClusterStartRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStartHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterStartHandlerService clusterStartHandlerService;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterStartRequest.class);
    }

    @Override
    public void accept(Event<ClusterStartRequest> event) {
        ClusterStartRequest request = event.getData();
        ClusterStartResult result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            CmTemplateProcessor blueprintProcessor = clusterStartHandlerService.getCmTemplateProcessor(stack);
            clusterStartHandlerService.startCluster(stack, blueprintProcessor, request.isDatahubRefreshNeeded());
            clusterStartHandlerService.handleStopStartScalingFeature(stack, blueprintProcessor);
            result = new ClusterStartResult(request);
        } catch (Exception e) {
            LOGGER.error("Exception happened during cluster start on cluster [{}]", request.getResourceId(), e);
            result = new ClusterStartResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
