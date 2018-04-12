package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.StackPreTerminationSuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;
import java.util.Set;

@Component
public class StackPreTerminationHandler implements ReactorEventHandler<StackPreTerminationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPreTerminationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public void accept(Event<StackPreTerminationRequest> requestEvent) {
        StackPreTerminationRequest request = requestEvent.getData();
        Stack stack = stackService.getByIdWithLists(request.getStackId());
        try {
            Cluster cluster = stack.getCluster();
            if (cluster != null) {
                Set<HostGroup> hostGroups = hostGroupService.getByCluster(cluster.getId());
                recipeEngine.executePreTerminationRecipes(stack, hostGroups);
            }
        } catch (Exception ex) {
            LOGGER.error("Pre-termination failed: {}", ex.getMessage(), ex);
        }

        Selectable result = new StackPreTerminationSuccess(stack.getId());
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StackPreTerminationRequest.class);
    }
}
