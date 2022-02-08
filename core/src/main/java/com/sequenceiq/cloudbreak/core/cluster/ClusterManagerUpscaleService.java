package com.sequenceiq.cloudbreak.core.cluster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;

@Service
public class ClusterManagerUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpscaleService.class);

    @Inject
    private StackService stackService;

    @Inject
    private ClusterHostServiceRunner hostRunner;

    @Inject
    private ClusterServiceRunner clusterServiceRunner;

    @Inject
    private ClusterService clusterService;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private TargetedUpscaleSupportService targetedUpscaleSupportService;

    public void upscaleClusterManager(Long stackId, String hostGroupName, Integer scalingAdjustment, boolean primaryGatewayChanged)
            throws ClusterClientInitException {
        Stack stack = stackService.getByIdWithListsInTransaction(stackId);
        LOGGER.debug("Adding {} new nodes for host group {}", scalingAdjustment, hostGroupName);
        Map<String, List<String>> hostsPerHostGroup = new HashMap<>();

        Map<String, String> hosts = hostRunner.addClusterServices(stackId, hostGroupName, scalingAdjustment);
        if (primaryGatewayChanged) {
            clusterServiceRunner.updateAmbariClientConfig(stack, stack.getCluster());
        }
        for (String hostName : hosts.keySet()) {
            if (!hostsPerHostGroup.containsKey(hostGroupName)) {
                hostsPerHostGroup.put(hostGroupName, new ArrayList<>());
            }
            hostsPerHostGroup.get(hostGroupName).add(hostName);
        }
        clusterService.updateInstancesToRunning(stack.getCluster().getId(), hostsPerHostGroup);

        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        ExtendedPollingResult result;
        if (!primaryGatewayChanged && targetedUpscaleSupportService.targetedUpscaleOperationSupported(stack)) {
            Set<Node> reachableCandidates = hostRunner.getReachableCandidates(stack, hosts);
            List<String> reachableCandidatesHostname = reachableCandidates.stream().map(Node::getHostname).collect(Collectors.toList());
            Set<InstanceMetaData> reachableInstances = stack.getNotDeletedInstanceMetaDataSet().stream()
                    .filter(md -> reachableCandidatesHostname.contains(md.getDiscoveryFQDN()))
                    .collect(Collectors.toSet());
            result = connector.waitForHosts(reachableInstances);
        } else {
            result = connector.waitForHosts(stackService.getByIdWithListsInTransaction(stackId).getRunningInstanceMetaDataSet());
        }
        if (result != null && result.isTimeout()) {
            LOGGER.info("Upscaling cluster manager were not successful for nodes: {}", result.getFailedInstanceIds());
            //instanceMetaDataService.updateInstanceStatus(result.getFailedInstanceIds(), InstanceStatus.ZOMBIE,
            //        "Upscaling cluster manager were not successful.";
        }
    }
}
