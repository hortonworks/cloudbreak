package com.sequenceiq.cloudbreak.reactor;

import com.sequenceiq.cloudbreak.domain.HostGroup;
import com.sequenceiq.cloudbreak.domain.HostMetadata;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.cluster.flow.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.bus.Event;
import reactor.bus.EventBus;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Component
public class DecommissionHandler implements ReactorEventHandler<DecommissionRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DecommissionHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private AmbariDecommissioner ambariDecommissioner;

    @Inject
    private RecipeEngine recipeEngine;

    @Inject
    private HostGroupService hostGroupService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(DecommissionRequest.class);
    }

    @Override
    public void accept(Event<DecommissionRequest> event) {
        DecommissionRequest request = event.getData();
        DecommissionResult result;
        try {
            Stack stack = stackService.getByIdWithLists(request.getStackId());
            Map<String, HostMetadata> hostsToRemove = ambariDecommissioner.collectHostsToRemove(stack, request.getHostGroupName(), request.getHostNames());
            Set<String> hostNames;
            if (!hostsToRemove.isEmpty()) {
                executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
                hostNames = ambariDecommissioner.decommissionAmbariNodes(stack, hostsToRemove);
            } else {
                hostNames = request.getHostNames();
            }
            result = new DecommissionResult(request, hostNames);
        } catch (Exception e) {
            result = new DecommissionResult(e.getMessage(), e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private void executePreTerminationRecipes(Stack stack, String hostGroupName, Collection<String> hostNames) {
        try {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(stack.getCluster().getId(), hostGroupName);
            recipeEngine.executePreTerminationRecipes(stack, Collections.singleton(hostGroup), hostNames);
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }
}
