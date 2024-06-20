package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.TerminationType;
import com.sequenceiq.cloudbreak.service.cluster.flow.PreTerminationStateExecutor;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.cluster.flow.telemetry.TelemetryAgentService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class StackPreTerminationHandler implements EventHandler<StackPreTerminationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPreTerminationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private TelemetryAgentService telemetryAgentService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private PreTerminationStateExecutor preTerminationStateExecutor;

    @Override
    public void accept(Event<StackPreTerminationRequest> requestEvent) {
        StackPreTerminationRequest request = requestEvent.getData();
        StackDto stackDto = stackDtoService.getById(request.getResourceId());
        try {
            ClusterView cluster = stackDto.getCluster();
            if (cluster != null && stackDto.getSecurityConfig() != null) {
                Set<HostGroup> hostGroupsWithRecipes = hostGroupService.getByClusterWithRecipes(cluster.getId());
                telemetryAgentService.stopTelemetryAgent(stackDto);
                recipeEngine.executePreTerminationRecipes(stackDto, hostGroupsWithRecipes, request.isForced());
                preTerminationStateExecutor.runPreTerminationTasks(stackDto);
            }
        } catch (Exception ex) {
            LOGGER.info("Pre-termination failed: {}", ex.getMessage(), ex);
        }

        Selectable result = new StackPreTerminationSuccess(stackDto.getStack().getId(), request.isForced() ? TerminationType.FORCED : TerminationType.REGULAR);
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StackPreTerminationRequest.class);
    }
}
