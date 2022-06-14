package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StartAmbariServicesHandler implements EventHandler<StartAmbariServicesRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(StartAmbariServicesHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StartAmbariServicesRequest.class);
    }

    @Override
    public void accept(Event<StartAmbariServicesRequest> event) {
        StartAmbariServicesRequest request = event.getData();
        Long stackId = request.getResourceId();
        Selectable response;
        try {
            clusterServiceRunner.runAmbariServices(stackId);
            Stack stack = stackService.getByIdWithListsInTransaction(stackId);
            clusterApiConnectors.getConnector(stack).waitForServer(stack, request.isDefaultClusterManagerAuth());
            response = new StartClusterManagerServicesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Start cluster manager services failed!", e);
            response = new StartAmbariServicesFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
