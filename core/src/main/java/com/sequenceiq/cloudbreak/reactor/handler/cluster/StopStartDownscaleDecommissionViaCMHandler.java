package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartds.StopStartDownscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartDownscaleDecommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartDownscaleDecommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class StopStartDownscaleDecommissionViaCMHandler extends ExceptionCatcherEventHandler<StopStartDownscaleDecommissionViaCMRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartDownscaleDecommissionViaCMHandler.class);

    private static final long POLL_FOR_10_MINUTES = TimeUnit.MINUTES.toSeconds(10);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopStartDownscaleDecommissionViaCMRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopStartDownscaleDecommissionViaCMRequest> event) {
        return new StackFailureEvent(StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FAIL_HANDLE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StopStartDownscaleDecommissionViaCMRequest> event) {
        StopStartDownscaleDecommissionViaCMRequest request = event.getData();
        LOGGER.info("StopStartDownscaleDecommissionViaCMHandler for: {}, {}", event.getData().getResourceId(), event.getData());

        try {
            Stack stack = request.getStack();
            Cluster cluster = stack.getCluster();
            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();

            Set<String> hostNames = getHostNamesForPrivateIds(request.getInstanceIdsToDecommission(), stack);
            LOGGER.debug("Attempting to decommission hosts. count={}, hostnames={}", hostNames.size(), hostNames);

            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), request.getHostGroupName())
                    .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));

            Map<String, InstanceMetaData> hostsToRemove = clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
            List<String> missingHostsInCm = Collections.emptyList();
            if (hostNames.size() != hostsToRemove.size()) {
                missingHostsInCm = hostNames.stream()
                        .filter(h -> !hostsToRemove.containsKey(h))
                        .collect(Collectors.toList());
                LOGGER.info("Found fewer instances in CM to decommission, as compared to initial ask. foundCount={}, initialCount={}, missingHostsInCm={}",
                        hostsToRemove.size(), hostNames.size(), missingHostsInCm);
            }

            // TODO CB-14929: Potentially put the nodes into maintenance mode before decommissioning?

            // TODO CB-15132: Eventually, try parsing the results of the CM decommission, and see if a partial decommission went through in the
            //  timebound specified.
            Set<String> decommissionedHostNames = Collections.emptySet();
            if (hostsToRemove.size() > 0) {
                decommissionedHostNames = clusterDecomissionService.decommissionClusterNodesStopStart(hostsToRemove, POLL_FOR_10_MINUTES);
                updateInstanceStatuses(hostsToRemove, decommissionedHostNames,
                        InstanceStatus.DECOMMISSIONED, "decommission requested for instances");
                // TODO CB-14929: Error Handling: In case of failures, figure out whcih nodes need to be moved into what is likely to be the
                //  ORCHESTRATION_FAILED state. This will likely be done in
            }

            // This doesn't handle failures. It handles scenarios where CM list APIs don't have the necessary hosts available.
            List<String> allMissingHostnames = null;
            if (missingHostsInCm.size() > 0) {
                allMissingHostnames = new LinkedList<>(missingHostsInCm);
            }
            if (hostsToRemove.size() != decommissionedHostNames.size()) {
                Set<String> finalDecommissionedHostnames = decommissionedHostNames;
                List<String> additionalMissingDecommissionHostnames = hostsToRemove.keySet().stream()
                        .filter(h -> !finalDecommissionedHostnames.contains(h))
                        .collect(Collectors.toList());
                LOGGER.info("Decommissioned fewer instances than requested. decommissionedCount={}, expectedCount={}, initialCount={}, notDecommissioned=[{}]",
                        decommissionedHostNames.size(), hostsToRemove.size(), hostNames.size(), additionalMissingDecommissionHostnames);
                if (allMissingHostnames == null) {
                    allMissingHostnames = new LinkedList<>();
                }
                allMissingHostnames.addAll(additionalMissingDecommissionHostnames);
            }

            LOGGER.info("hostsDecommissioned: count={}, hostNames={}", decommissionedHostNames.size(), decommissionedHostNames);

            if (decommissionedHostNames.size() > 0) {
                LOGGER.debug("Attempting to put decommissioned hosts into maintenance mode. count={}", decommissionedHostNames.size());
                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTERINGCMMAINTMODE,
                        String.valueOf(decommissionedHostNames.size()));

                clusterDecomissionService.enterMaintenanceMode(stack, decommissionedHostNames);

                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_DOWNSCALE_ENTEREDCMMAINTMODE,
                        String.valueOf(decommissionedHostNames.size()));
                LOGGER.debug("Successfully put decommissioned hosts into maintenance mode. count={}", decommissionedHostNames.size());
            } else {
                LOGGER.debug("No nodes decommissioned, hence no nodes being put into maintenance mode");
            }

            // TODO CB-15132: Maybe consider a CM API to propaget a node decommission timeout -
            //  separation between a forced downscale (random nodes), and a specified list of nodes
            //  The main differentiation is whether the nodes are expected to be running 'old' work,
            //  or are safe to remove fast (i.e. AutoScale downscale - race could only put 20-30s odd worth of work on the new nodes).

            StopStartDownscaleDecommissionViaCMResult result =
                    new StopStartDownscaleDecommissionViaCMResult(request, decommissionedHostNames, allMissingHostnames);
            return result;
        } catch (Exception e) {
            // TODO CB-15132: This can be improved based on where and when the Exception occurred to potentially rollback certain aspects.
            // ClusterClientInitException is one which is explicitly thrown.
            return new StackFailureEvent(StopStartDownscaleEvent.STOPSTART_DOWNSCALE_FAIL_HANDLE_EVENT.event(), request.getResourceId(), e);
        }
    }

    private Set<String> getHostNamesForPrivateIds(Set<Long> hostIdsToRemove, Stack stack) {
        return hostIdsToRemove.stream().map(privateId -> {
            Optional<InstanceMetaData> instanceMetadata = stackService.getInstanceMetadata(stack.getInstanceMetaDataAsList(), privateId);
            return instanceMetadata.map(InstanceMetaData::getDiscoveryFQDN).orElse(null);
        }).filter(StringUtils::isNotEmpty).collect(Collectors.toSet());
    }

    private void updateInstanceStatuses(Map<String, InstanceMetaData> instanceMetadataMap, Set<String> fqdnsRemoved,
            InstanceStatus instanceStatus, String statusReason) {
        for (String fqdn : fqdnsRemoved) {
            InstanceMetaData instanceMetaData = instanceMetadataMap.get(fqdn);
            if (instanceMetaData == null) {
                throw new RuntimeException(
                        String.format("Unexpected fqdn decommissioned. Not present in requsted list. unexpected fqdn=[%s], ExpectedSet=[%s]",
                                fqdn, instanceMetadataMap));
            }
            instanceMetaDataService.updateInstanceStatus(instanceMetaData, instanceStatus, statusReason);
        }
    }
}