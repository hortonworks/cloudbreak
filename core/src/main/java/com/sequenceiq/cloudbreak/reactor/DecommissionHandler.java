package com.sequenceiq.cloudbreak.reactor;

import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.DECOMMISSION_ERROR_PHASE;
import static com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult.UNKNOWN_ERROR_PHASE;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostMetadata;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DecommissionResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.cluster.ambari.AmbariDecommissioner;
import com.sequenceiq.cloudbreak.service.cluster.ambari.DecommissionException;
import com.sequenceiq.cloudbreak.service.cluster.flow.recipe.RecipeEngine;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

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
        String hostGroupName = request.getHostGroupName();
        Set<String> hostNames = Collections.emptySet();
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getStackId());
            hostNames = getHostNamesForPrivateIds(request, stack);
            if (!skipAmbariDecomission(request, hostNames)) {
                Map<String, HostMetadata> hostsToRemove = ambariDecommissioner.collectHostsToRemove(stack, hostGroupName, hostNames);
                executePreTerminationRecipes(stack, request.getHostGroupName(), hostsToRemove.keySet());
                ambariDecommissioner.decommissionAmbariNodes(stack, hostsToRemove);
            }
            result = new DecommissionResult(request, hostNames);
        } catch (DecommissionException e) {
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, DECOMMISSION_ERROR_PHASE);
        } catch (Exception e) {
            result = new DecommissionResult(e.getMessage(), e, request, hostNames, UNKNOWN_ERROR_PHASE);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }

    private boolean skipAmbariDecomission(DecommissionRequest request, Set<String> hostNames) {
        return hostNames.isEmpty() || request.getDetails() != null && request.getDetails().isForced();
    }

    private Set<String> getHostNamesForPrivateIds(DecommissionRequest request, Stack stack) {
        return request.getPrivateIds().stream().map(privateId -> {
            Optional<InstanceMetaData> instanceMetadata = stackService.getInstanceMetadata(stack.getInstanceMetaDataAsList(), privateId);
            return instanceMetadata.map(InstanceMetaData::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private void executePreTerminationRecipes(Stack stack, String hostGroupName, Set<String> hostNames) {
        try {
            HostGroup hostGroup = hostGroupService.getByClusterIdAndNameWithRecipes(stack.getCluster().getId(), hostGroupName);
            recipeEngine.executePreTerminationRecipes(stack, Collections.singleton(hostGroup), hostNames);
        } catch (Exception ex) {
            LOGGER.error(ex.getLocalizedMessage(), ex);
        }
    }
}
