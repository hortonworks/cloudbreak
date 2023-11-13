package com.sequenceiq.cloudbreak.core.cluster;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cluster.service.ClusterClientInitException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorException;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Memory;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;

@Component
public class ClusterManagerDefaultConfigAdjuster {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManagerDefaultConfigAdjuster.class);

    private static final double MAX_CM_MEMORY_USAGE_FACTOR = 0.25;

    private static final Memory FOUR_GIGABYTES = Memory.ofGigaBytes(4);

    private static final Memory THIRTY_TWO_GIGABYTES = Memory.ofGigaBytes(32);

    private static final int ONE_HUNDRED = 100;

    private static final int MIN_CM_MEMORY_NODE_COUNT = ONE_HUNDRED;

    private static final int MIN_OPERATION_TIMEOUT_CHANGE_NODE_COUNT = 2 * ONE_HUNDRED;

    private static final int MAX_CM_MEMORY_NODE_COUNT = 800;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private CloudbreakEventService cloudbreakEventService;

    public void adjustDefaultConfig(StackDto stackDto, int nodeCount) {
        if (!StackType.WORKLOAD.equals(stackDto.getType())) {
            LOGGER.info("CM adjustment is skipped. It is only supported for data hub clusters.");
            return;
        }
        try {
            GatewayConfig gateway = gatewayConfigService.getPrimaryGatewayConfig(stackDto);
            boolean operationTimeoutChanged = setClusterManagerOperationTimeout(nodeCount, gateway);
            boolean memorySettingsChanged = setMemory(stackDto, nodeCount, gateway);
            if (operationTimeoutChanged || memorySettingsChanged) {
                restartClusterManager(gateway, stackDto);
                witForClusterManagerToBecomeAvailable(stackDto);
            }
        } catch (Exception e) {
            LOGGER.error("Error happened while tried to configure cluster manager memory.", e);
        }
    }

    private boolean setMemory(StackDto stackDto, int nodeCount, GatewayConfig gateway) throws CloudbreakOrchestratorFailedException {
        Memory currentMemory = getClusterManagerMemory(gateway);
        Memory requiredMemory = calcRequiredClusterManagerMemory(nodeCount);
        if (requiredMemory.getValueInBytes() > currentMemory.getValueInBytes()) {
            Memory availableMemory = getAvailableMemory(gateway);
            Memory maxAllowedMemory = Memory.ofGigaBytes((int) (availableMemory.getValueInGigaBytes() * MAX_CM_MEMORY_USAGE_FACTOR));
            if (requiredMemory.getValueInBytes() > maxAllowedMemory.getValueInBytes()) {
                sendWarningToUser(stackDto, requiredMemory, availableMemory);
                requiredMemory = maxAllowedMemory;
            }

            if (requiredMemory.getValueInBytes() > currentMemory.getValueInBytes()) {
                LOGGER.info("Updating cluster manager memory. Current value: {}, Target value: {}, Available memory: {}",
                        currentMemory, requiredMemory, availableMemory);
                updateClusterManagerMemoryConfig(gateway, requiredMemory);
                return true;
            }
        } else {
            LOGGER.info("Cluster manager's configured memory {}GB is enough for {} nodes.", currentMemory, nodeCount);
        }
        return false;
    }

    private Memory getClusterManagerMemory(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException {
        return hostOrchestrator.getClusterManagerMemory(gatewayConfig)
                .orElseThrow(() -> new CloudbreakRuntimeException("Couldn't get cloudera manager memory."));
    }

    private void witForClusterManagerToBecomeAvailable(StackDto stackDto) throws ClusterClientInitException, CloudbreakException {
        clusterApiConnectors.getConnector(stackDto).waitForServer(false);
    }

    private void restartClusterManager(GatewayConfig gatewayConfig, StackDto stackDto) throws CloudbreakOrchestratorException {
        hostOrchestrator.restartClusterManagerOnMaster(
                gatewayConfig,
                Set.of(gatewayConfig.getHostname()),
                ClusterDeletionBasedExitCriteriaModel.clusterDeletionBasedModel(stackDto.getId(), stackDto.getCluster().getId()));
    }

    private void updateClusterManagerMemoryConfig(GatewayConfig gatewayConfig, Memory requiredMemory) throws CloudbreakOrchestratorFailedException {
        hostOrchestrator.setClusterManagerMemory(gatewayConfig, requiredMemory);
    }

    private boolean setClusterManagerOperationTimeout(int nodeCount, GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException {
        if (nodeCount > MIN_OPERATION_TIMEOUT_CHANGE_NODE_COUNT) {
            return hostOrchestrator.setClouderaManagerOperationTimeout(gatewayConfig);
        } else {
            return false;
        }
    }

    private Memory getAvailableMemory(GatewayConfig gatewayConfig) throws CloudbreakOrchestratorFailedException {
        return hostOrchestrator.getMemoryInfo(gatewayConfig)
                .orElseThrow(() -> new CloudbreakRuntimeException("Couldn't get memory information."))
                .getTotalMemory();
    }

    private Memory calcRequiredClusterManagerMemory(int nodeCount) {
        if (nodeCount < MIN_CM_MEMORY_NODE_COUNT) {
            return FOUR_GIGABYTES;
        }
        if (nodeCount >= MAX_CM_MEMORY_NODE_COUNT) {
            return THIRTY_TWO_GIGABYTES;
        }
        return new Memory(FOUR_GIGABYTES.getValueInBytes() * Math.floorDiv(nodeCount, ONE_HUNDRED));
    }

    private void sendWarningToUser(StackDto stackDto, Memory requiredMemory, Memory availableMemory) {
        cloudbreakEventService.fireCloudbreakEvent(
                stackDto.getId(),
                Status.UPDATE_IN_PROGRESS.name(),
                ResourceEvent.CLUSTER_SCALING_CM_MEMORY_WARNING,
                List.of(String.format("%.2f", requiredMemory.getValueInGigaBytes()),
                        String.format("%.2f", availableMemory.getValueInGigaBytes())));
    }
}
