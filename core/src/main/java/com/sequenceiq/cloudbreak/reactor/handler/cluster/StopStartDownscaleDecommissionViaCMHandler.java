package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE;

import java.util.Collection;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStartDownscaleDecommissionViaCMHandler implements EventHandler<StopStartDownscaleDecommissionViaCMRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleDecommissionViaCMHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    // TODO CB-14929: Should flowMessageService be used inside a hnadler to write messages to the activity log, etc.
    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopStartDownscaleDecommissionViaCMRequest.class);
    }

    @Override
    public void accept(Event<StopStartDownscaleDecommissionViaCMRequest> event) {
        StopStartDownscaleDecommissionViaCMRequest request = event.getData();
        LOGGER.info("StopStartDownscaleDecommissionViaCMHandler for: {}, {}", event.getData().getResourceId(), event);

        try {
            Stack stack = request.getStack();
            Cluster cluster = stack.getCluster();
            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();

            Set<String> hostNames = getHostNamesForPrivateIds(request.getInstanceIdsToDecommission(), stack);
            LOGGER.info("ZZZ: hostNamesToDecommission: count={}, hostNames={}", hostNames.size(), hostNames);

            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), request.getHostGroupName())
                    .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));

            Map<String, InstanceMetaData> hostsToRemove = clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
            LOGGER.info("ZZZ: hostNamesToDecommission after checking with CM: count={}, details={}", hostsToRemove.size(), hostsToRemove);

            // TODO CB-14929: Potentially put the nodes into maintenance mode before decommissioning?

            // TODO CB-14929: Time bound commission. Potentially poll for nodes which do not reach the desired state within a time bound.
            Set<String> decommissionedHostNames = Collections.emptySet();
            if (hostsToRemove.size() > 0) {
                updateInstanceStatuses(hostsToRemove.values(), InstanceStatus.DECOMMISSIONED, "decommission requested for instance");
                decommissionedHostNames = clusterDecomissionService.decommissionClusterNodes(hostsToRemove);
            }
            LOGGER.info("ZZZ: hostsDecommissioned: count={}, hostNames={}", decommissionedHostNames.size(), decommissionedHostNames);

            LOGGER.info("ZZZ: Attempting to put hosts into maintenance mode");
            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE,
                    String.valueOf(hostsToRemove.size()));

            clusterDecomissionService.enterMaintenanceMode(stack, hostsToRemove);

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE,
                    String.valueOf(hostsToRemove.size()));
            LOGGER.info("ZZZ: Nodes moved to maintenance mode");


            // TODO CB-14929: Maybe consider a CM API to propaget a node decommission timeout -
            //  separation between a forced downscale (random nodes), and a specified list of nodes
            //  The main differentiation is whether the nodes are expected to be running 'old' work,
            //  or are safe to remove fast (i.e. AutoScale downscale - race could only put 20-30s odd worth of work on the new nodes).
            // TODO CB-14929: Populate the status of the nodes which were stopped successfully, those which failed/timed out, etc.
            StopStartDownscaleDecommissionViaCMResult result = new StopStartDownscaleDecommissionViaCMResult(request, decommissionedHostNames);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } finally {
            LOGGER.debug("ZZZ: Remove this. Added for checkstyles");
            // TODO CB-14929: Proper exception handling in a catch block
        }
    }

    private Set<String> getHostNamesForPrivateIds(Set<Long> hostIdsToRemove, Stack stack) {
        // List<String> decomissionedHostNames = stackService.getHostNamesForPrivateIds(stack.getInstanceMetaDataAsList(),
        // request.getInstanceIdsToDecommission());
        return hostIdsToRemove.stream().map(privateId -> {
            Optional<InstanceMetaData> instanceMetadata = stackService.getInstanceMetadata(stack.getInstanceMetaDataAsList(), privateId);
            return instanceMetadata.map(InstanceMetaData::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private void updateInstanceStatuses(Collection<InstanceMetaData> instanceMetadatas, InstanceStatus instanceStatus, String statusReason) {
        for (InstanceMetaData instanceMetaData : instanceMetadatas) {
            instanceMetaDataService.updateInstanceStatus(instanceMetaData, instanceStatus, statusReason);
        }
    }
}
