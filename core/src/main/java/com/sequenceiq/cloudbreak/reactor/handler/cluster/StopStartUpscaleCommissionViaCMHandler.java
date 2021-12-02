package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_SCALING_STOPSTART_UPSCALE_WAITING_HOSTSTART;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterDecomissionService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterSetupService;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.StopStartUpscaleCommissionViaCMRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.StopStartUpscaleCommissionViaCMResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class StopStartUpscaleCommissionViaCMHandler implements EventHandler<StopStartUpscaleCommissionViaCMRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(StopStartUpscaleCommissionViaCMHandler.class);

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
        return EventSelectorUtil.selector(StopStartUpscaleCommissionViaCMRequest.class);
    }

    @Override
    public void accept(Event<StopStartUpscaleCommissionViaCMRequest> event) {
        StopStartUpscaleCommissionViaCMRequest request = event.getData();
        LOGGER.info("StopStartUpscaleCommissionViaCMHandler for: {}, {}", event.getData().getResourceId(), event);
        LOGGER.info("ZZZ: InstancesToCommissionViaCM: {}", request.getInstancesToCommission());

        try {
            Stack stack = request.getStack();
            Cluster cluster = stack.getCluster();

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_UPSCALE_WAITING_HOSTSTART,
                    String.valueOf(request.getInstancesToCommission().size()));

            ClusterSetupService clusterSetupService = clusterApiConnectors.getConnector(stack).clusterSetupService();
            clusterSetupService.waitForHosts2(new HashSet(request.getInstancesToCommission()));

            flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_SCALING_STOPSTART_UPSCALE_CMHOSTSSTARTED,
                    String.valueOf(request.getInstancesToCommission().size()));

            ClusterDecomissionService clusterDecomissionService = clusterApiConnectors.getConnector(stack).clusterDecomissionService();

            // TODO CB-14929:  No null fqdn etc checking in place. Rant:  Java Streams are terrible to easily get things wrong,
            // and not think through what could potetntially braek. Not to mention the syntax..
            Set<String> hostNames = request.getInstancesToCommission().stream().map(x -> x.getDiscoveryFQDN()).collect(Collectors.toSet());
            LOGGER.info("ZZZ: hostNamesToRecommission: count={}, hostNames={}", hostNames.size(), hostNames);

            HostGroup hostGroup = hostGroupService.getByClusterIdAndName(cluster.getId(), request.getHostGroupName())
                    .orElseThrow(NotFoundException.notFound("hostgroup", request.getHostGroupName()));

            Map<String, InstanceMetaData> hostsToRecommission = clusterDecomissionService.collectHostsToRemove(hostGroup, hostNames);
            LOGGER.info("ZZZ: hostNamesToRecommission after checking with CM: count={}, details={}", hostsToRecommission.size(), hostsToRecommission);

            // TODO CB-14929: Ensure CM, relevant services (YARN RM) are in a functional state - or fail/delay the operation

            // TODO CB-14929: Potentially poll nodes for success. Don't fail the entire operation if a single node fails to commission.
            //  What would need to happen to the CM command in this case? (Can only work in the presence of a co-operative CM API call.
            //  Alternately this could go straight to the service)

            Set<String> recommissionedHostnames = Collections.emptySet();
            if (hostsToRecommission.size() > 0) {
                recommissionedHostnames = clusterDecomissionService.recommissionClusterNodes(hostsToRecommission);
            }
            LOGGER.info("ZZZ: hostsRecommissioned: count={}, hostNames={}", recommissionedHostnames.size(), recommissionedHostnames);

            // TODO CB-14929: Wait for services to start / force CM sync.

            // TODO CB-14929: Include enough information about successful/failed nodes in the result.
            StopStartUpscaleCommissionViaCMResult result = new StopStartUpscaleCommissionViaCMResult(request);
            eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
        } catch (ClusterClientInitException e) {
            throw new RuntimeException(e);
        }
    }
}
