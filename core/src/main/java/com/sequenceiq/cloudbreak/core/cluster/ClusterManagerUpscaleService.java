package com.sequenceiq.cloudbreak.core.cluster;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_NODES_MARKED_AS_ZOMBIE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_NODE_FAILURE_REASON_UNREACHABLE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_NODE_FAILURE_REASON_WAIT_FOR_HOSTS_TIMEOUT;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
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

    @Inject
    private ClusterManagerDefaultConfigAdjuster clusterManagerDefaultConfigAdjuster;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private CloudbreakMessagesService messagesService;

    public void upscaleClusterManager(Long stackId, Map<String, Integer> hostGroupWithAdjustment, boolean primaryGatewayChanged, boolean repair)
            throws ClusterClientInitException {
        StackDto stackDto = stackDtoService.getById(stackId);

        StackView stack = stackDto.getStack();

        LOGGER.debug("Adding new nodes for host group {}", hostGroupWithAdjustment);
        NodeReachabilityResult nodeReachabilityResult = hostRunner.addClusterServices(stackDto, hostGroupWithAdjustment, repair);
        String clusterManagerIp = stackDto.getClusterManagerIp();
        if (primaryGatewayChanged) {
            clusterManagerIp = clusterServiceRunner.updateClusterManagerClientConfig(stackDto);
            if (stackDto.isOnGovPlatformVariant()) {
                hostRunner.removeSecurityConfigFromCMAgentsConfig(stackDto, nodeReachabilityResult.getReachableNodes());
            }
        }
        clusterService.updateInstancesToRunning(stackId, nodeReachabilityResult.getReachableNodes());
        clusterService.updateInstancesToZombie(stackId, nodeReachabilityResult.getUnreachableNodes(), CLUSTER_NODE_FAILURE_REASON_UNREACHABLE);

        clusterManagerDefaultConfigAdjuster.adjustDefaultConfig(stackDto, stackDto.getNotDeletedInstanceMetaData().size(), false);

        ClusterApi connector = clusterApiConnectors.getConnector(stackDto, clusterManagerIp);
        ExtendedPollingResult result;
        boolean targetedUpscaleAvailable = isTargetedUpscaleAvailable(primaryGatewayChanged, repair, stack);
        if (targetedUpscaleAvailable) {
            Set<String> reachableHosts = nodeReachabilityResult.getReachableHosts();
            Set<InstanceMetadataView> reachableInstances = stackDto.getAllAvailableInstances().stream()
                    .filter(md -> reachableHosts.contains(md.getDiscoveryFQDN()))
                    .collect(Collectors.toSet());
            result = connector.waitForHosts(reachableInstances);
        } else {
            result = connector.waitForHosts(stackDto.getRunningInstanceMetaDataSet());
        }
        if (result != null && result.isTimeout()) {
            handleWaitForHostsTimeout(targetedUpscaleAvailable, stack, result);
        }
    }

    private boolean isTargetedUpscaleAvailable(boolean primaryGatewayChanged, boolean repair, StackView stack) {
        return !repair && !primaryGatewayChanged && targetedUpscaleSupportService.targetedUpscaleOperationSupported(stack);
    }

    private void handleWaitForHostsTimeout(boolean targetedUpscaleAvailable, StackView stack, ExtendedPollingResult result) {
        String errorMessage = getTimeoutResultMessage(result);
        LOGGER.warn(errorMessage);
        Set<Long> failedInstanceIds = collectFailedInstanceIds(stack, result);
        String statusReason = messagesService.getMessage(CLUSTER_NODE_FAILURE_REASON_WAIT_FOR_HOSTS_TIMEOUT.getMessage());
        if (targetedUpscaleAvailable) {
            instanceMetaDataService.updateInstanceStatuses(failedInstanceIds, InstanceStatus.ZOMBIE, statusReason);
            if (CollectionUtils.isNotEmpty(failedInstanceIds)) {
                String zombieNodes = String.join(", ", collectFailedInstanceIdentifiers(stack, failedInstanceIds));
                flowMessageService.fireEventAndLog(stack.getId(), UPDATE_IN_PROGRESS.name(), CLUSTER_NODES_MARKED_AS_ZOMBIE, zombieNodes, statusReason);
            }
        } else {
            instanceMetaDataService.updateInstanceStatuses(failedInstanceIds, InstanceStatus.ORCHESTRATION_FAILED, statusReason);
        }

        if (!targetedUpscaleAvailable || CollectionUtils.isEmpty(result.getFailedInstancePrivateIds())) {
            throw new CloudbreakServiceException(errorMessage, result.getException());
        }
    }

    private String getTimeoutResultMessage(ExtendedPollingResult result) {
        StringBuilder errorMessage = new StringBuilder("Upscaling cluster manager was not successful, waiting for hosts timed out");
        if (CollectionUtils.isNotEmpty(result.getFailedInstancePrivateIds())) {
            errorMessage.append(" for nodes: ").append(result.getFailedInstancePrivateIds());
        }
        if (result.getException() != null) {
            errorMessage.append(", reason: ").append(result.getException().getMessage());
        } else {
            errorMessage.append(", please check Cloudera Manager logs for further details.");
        }
        return errorMessage.toString();
    }

    private Set<Long> collectFailedInstanceIds(StackView stack, ExtendedPollingResult result) {
        Set<Long> failedInstanceIds = new HashSet<>();
        if (CollectionUtils.isNotEmpty(result.getFailedInstancePrivateIds())) {
            failedInstanceIds.addAll(result.getFailedInstancePrivateIds());
        } else {
            List<InstanceMetadataView> allInstanceMetadataInServicesRunningStatus = instanceMetaDataService.getAllStatusInForStack(
                    stack.getId(), Set.of(InstanceStatus.SERVICES_RUNNING));
            Set<Long> instanceIdsInServicesRunningStatus = allInstanceMetadataInServicesRunningStatus.stream()
                    .map(instanceMetadataView -> instanceMetadataView.getId()).collect(Collectors.toSet());
            failedInstanceIds.addAll(instanceIdsInServicesRunningStatus);
        }
        return failedInstanceIds;
    }

    private List<String> collectFailedInstanceIdentifiers(StackView stack, Set<Long> failedInstanceIds) {
        return instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stack.getId()).stream()
                .filter(instanceMetadataView -> failedInstanceIds.contains(instanceMetadataView.getId()))
                .map(instanceMetadataView -> instanceMetadataView.getDiscoveryFQDN() != null
                        ? instanceMetadataView.getDiscoveryFQDN()
                        : instanceMetadataView.getInstanceId() != null
                                ? instanceMetadataView.getInstanceId() : String.valueOf(instanceMetadataView.getId()))
                .collect(Collectors.toList());
    }
}
