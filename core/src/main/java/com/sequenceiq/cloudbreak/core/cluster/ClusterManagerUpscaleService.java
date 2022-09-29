package com.sequenceiq.cloudbreak.core.cluster;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
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
        clusterService.updateInstancesToRunning(stackId, nodeReachabilityResult.getReachableNodes());
        clusterService.updateInstancesToZombie(stackId, nodeReachabilityResult.getUnreachableNodes());

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
            handleWaitForHostsTimeout(repair, primaryGatewayChanged, stack, result);
        }
    }

    private void handleWaitForHostsTimeout(boolean repair, boolean primaryGatewayChanged, StackView stack, ExtendedPollingResult result) {
        if (CollectionUtils.isNotEmpty(result.getFailedInstanceIds())) {
            String message = String.format("Upscaling cluster manager were not successful, waiting for hosts timed out for nodes: %s",
                    result.getFailedInstanceIds());
            LOGGER.warn(message);
            if (!repair && !primaryGatewayChanged && targetedUpscaleSupportService.targetedUpscaleOperationSupported(stack)) {
                instanceMetaDataService.updateInstanceStatuses(result.getFailedInstanceIds(), InstanceStatus.ZOMBIE,
                        "Upscaling cluster manager were not successful, waiting for hosts timed out");
            } else {
                instanceMetaDataService.updateInstanceStatuses(result.getFailedInstanceIds(), InstanceStatus.ORCHESTRATION_FAILED,
                        "Upscaling cluster manager were not successful, waiting for hosts timed out");
                throw new CloudbreakServiceException(message);
            }
        } else if (result.getException() != null) {
            // if getFailedInstanceIds is null, then possibly something is wrong with CM, so we should throw exception anyway
            LOGGER.error("Upscaling cluster manager were not successful, waiting for hosts timed out because: ", result.getException());
            throw new CloudbreakServiceException(result.getException());
        } else {
            LOGGER.error("Upscaling cluster manager were not successful, waiting for hosts timed out!");
            throw new CloudbreakServiceException("Upscaling cluster manager were not successful, waiting for hosts timed out, " +
                    "please check Cloudera Manager logs fur further details.");
        }
    }
}