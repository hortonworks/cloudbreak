package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_WAITING_HOSTSTART;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterCommissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.stopstartus.StopStartUpscaleEvent;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartUpscaleCommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

import reactor.bus.Event;

@Component
public class StopStartUpscaleCommissionViaCMHandler extends ExceptionCatcherEventHandler<StopStartUpscaleCommissionViaCMRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleCommissionViaCMHandler.class);

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(StopStartUpscaleCommissionViaCMRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<StopStartUpscaleCommissionViaCMRequest> event) {
        return new StackFailureEvent(StopStartUpscaleEvent.STOPSTART_UPSCALE_FAILURE_EVENT.event(), resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<StopStartUpscaleCommissionViaCMRequest> event) {
        StopStartUpscaleCommissionViaCMRequest request = event.getData();
        LOGGER.info("StopStartUpscaleCommissionViaCMHandler for: {}, {}", event.getData().getResourceId(), event);
        LOGGER.debug("StartedInstancesToCommission: {}, servicesNotRunningInstancesToCommission: {}",
                request.getStartedInstancesToCommission(),
                request.getServicesNotRunningInstancesToCommission());

        List<InstanceMetaData> allInstancesToCommission = new LinkedList<>();
        allInstancesToCommission.addAll(request.getStartedInstancesToCommission());
        allInstancesToCommission.addAll(request.getServicesNotRunningInstancesToCommission());

        try {
            Stack stack = request.getStack();
            Cluster cluster = stack.getCluster();

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_UPSCALE_WAITING_HOSTSTART,
                    String.valueOf(allInstancesToCommission.size()));

            ClusterSetupService clusterSetupService = clusterApiConnectors.getConnector(stack).clusterSetupService();
            clusterSetupService.waitForHostsHealthy(new HashSet(allInstancesToCommission));

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED,
                    String.valueOf(allInstancesToCommission.size()));

            ClusterCommissionService clusterCommissionService = clusterApiConnectors.getConnector(stack).clusterCommissionService();

            Set<String> hostNames = allInstancesToCommission.stream().map(i -> i.getDiscoveryFQDN()).collect(Collectors.toSet());
            LOGGER.debug("HostNames to recommission: count={}, hostNames={}", hostNames.size(), hostNames);

            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), request.getHostGroupName())
                    .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));

            Map<String, InstanceMetaData> hostsToRecommission = clusterCommissionService.collectHostsToCommission(hostGroup, hostNames);
            List<String> missingHostsInCm = Collections.emptyList();
            if (hostNames.size() != hostsToRecommission.size()) {
                missingHostsInCm = hostNames.stream()
                        .filter(h -> !hostsToRecommission.containsKey(h))
                        .collect(Collectors.toList());
                LOGGER.info("Found fewer instances in CM to commission, as compared to initial ask. foundCount={}, initialCount={}, missingHostsInCm={}",
                        hostsToRecommission.size(), hostNames.size(), missingHostsInCm);
            }

            // TODO CB-14929: Eventually ensure CM, relevant services (YARN RM) are in a functional state - or fail/delay the operation

            // TODO CB-15132: Potentially poll nodes for success. Don't fail the entire operation if a single node fails to commission.
            //  What would need to happen to the CM command in this case? (Can only work in the presence of a co-operative CM API call.
            //  Alternately this could go straight to the service)

            Set<String> recommissionedHostnames = Collections.emptySet();
            if (hostsToRecommission.size() > 0) {
                recommissionedHostnames = clusterCommissionService.recommissionClusterNodes(hostsToRecommission);
                // TODO CB-14929: Maybe wait for services to start / force CM sync.
            }
            List<String> allMissingRecommissionHostnames = null;
            if (missingHostsInCm.size() > 0) {
                allMissingRecommissionHostnames = new LinkedList<>(missingHostsInCm);
            }
            if (hostsToRecommission.size() != recommissionedHostnames.size()) {
                Set<String> finalRecommissionedHostnames = recommissionedHostnames;
                List<String> additionalMissingRecommissionHostnames = hostsToRecommission.keySet().stream()
                        .filter(h -> !finalRecommissionedHostnames.contains(h))
                        .collect(Collectors.toList());
                LOGGER.info("Recommissioned fewer instances than requested. recommissionedCount={}, expectedCount={}, initialCount={}, notRecommissioned=[{}]",
                        recommissionedHostnames.size(), hostsToRecommission.size(), hostNames.size(), additionalMissingRecommissionHostnames);
                if (allMissingRecommissionHostnames == null) {
                    allMissingRecommissionHostnames = new LinkedList<>();
                }
                allMissingRecommissionHostnames.addAll(additionalMissingRecommissionHostnames);
            }

            StopStartUpscaleCommissionViaCMResult result =
                    new StopStartUpscaleCommissionViaCMResult(request, recommissionedHostnames, allMissingRecommissionHostnames);
            return result;
        } catch (Exception e) {
            // TODO CB-15132: This can be improved based on where and when the Exception occurred to potentially rollback certain aspects.
            // ClusterClientInitException is one which is explicitly thrown.
            return new StackFailureEvent(StopStartUpscaleEvent.STOPSTART_UPSCALE_FAILURE_EVENT.event(), request.getResourceId(), e);
        }
    }
}