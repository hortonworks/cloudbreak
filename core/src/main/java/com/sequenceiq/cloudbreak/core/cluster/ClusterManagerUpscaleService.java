package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterServiceRunner;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.NodeReachabilityResult;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.TargetedUpscaleSupportService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.cloudbreak.view.StackView;

@Service
public class ClusterManagerUpscaleService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerUpscaleService.class);

    @Inject
    private StackDtoService stackDtoService;

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

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void upscaleClusterManager(Long stackId, Map<String, Integer> hostGroupWithAdjustment, boolean primaryGatewayChanged, boolean repair)
            throws ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);

        StackView stack = stackDto.getStack();

        LOGGER.debug("Adding new nodes for host group {}", hostGroupWithAdjustment);

        NodeReachabilityResult nodeReachabilityResult = hostRunner.addClusterServices(stackDto, hostGroupWithAdjustment, repair);
        String clusterManagerIp = stackDto.getClusterManagerIp();
        if (primaryGatewayChanged) {
            clusterManagerIp = clusterServiceRunner.updateAmbariClientConfig(stackDto);
        }
        clusterService.updateInstancesToRunning(stack.getId(), nodeReachabilityResult.getReachableNodes());
        clusterService.updateInstancesToZombie(stack.getId(), nodeReachabilityResult.getUnreachableNodes());

        ClusterApi connector = clusterApiConnectors.getConnector(stackDto, clusterManagerIp);
        ExtendedPollingResult result;
        if (!repair && !primaryGatewayChanged && targetedUpscaleSupportService.targetedUpscaleOperationSupported(stack)) {
            Set<String> reachableHosts = nodeReachabilityResult.getReachableHosts();
            Set<InstanceMetadataView> reachableInstances = stackDto.getAllAvailableInstances().stream()
                    .filter(md -> reachableHosts.contains(md.getDiscoveryFQDN()))
                    .collect(Collectors.toSet());
            result = connector.waitForHosts(reachableInstances);
        } else {
            result = connector.waitForHosts(stackDto.getRunningInstanceMetaDataSet());
        }
        if (result != null && result.isTimeout()) {
            LOGGER.info("Upscaling cluster manager were not successful for nodes: {}", result.getFailedInstanceIds());
            instanceMetaDataService.updateInstanceStatuses(result.getFailedInstanceIds(), InstanceStatus.ZOMBIE,
                    "Upscaling cluster manager were not successful.");
        }
    }
}
