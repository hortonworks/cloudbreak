package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartAmbariServicesRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StartClusterManagerServicesSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

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
    private StackDtoService stackDtoService;

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
            StackDto stack = stackDtoService.getById(stackId);
            String clusterManagerIp = clusterServiceRunner.updateClusterManagerClientConfig(stack);
            clusterServiceRunner.runClusterManagerServices(stack, request.isRunPreServiceDeploymentRecipe());
            clusterApiConnectors.getConnector(stack, clusterManagerIp).waitForServer(request.isDefaultClusterManagerAuth());
            response = new StartClusterManagerServicesSuccess(stackId);
        } catch (Exception e) {
            LOGGER.info("Start cluster manager services failed!", e);
            response = new StartAmbariServicesFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
