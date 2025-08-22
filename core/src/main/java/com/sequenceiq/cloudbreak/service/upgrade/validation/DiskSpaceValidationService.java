package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.STOPPED;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorRunParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;

@Component
public class DiskSpaceValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSpaceValidationService.class);

    private static final long DIVIDER_TO_MB = 1024L;

    private static final long GB_IN_KB = 1048576;

    private static final double GATEWAY_NODE_PARCEL_SIZE_MULTIPLIER = 3.5;

    private static final double PARCEL_SIZE_MULTIPLIER = 2.5;

    private static final String DISK_FREE_SPACE_COMMAND = "df -k / | tail -1 | awk '{print $4}'";

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ResourceService resourceService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void validateFreeSpaceForUpgrade(Stack stack, long requiredFreeSpace) {
        Map<String, Double> freeDiskSpaceByNodes = getFreeDiskSpaceByNodes(stack);
        LOGGER.debug("Required free space for parcels {} KB. Free space by nodes in KB: {}", requiredFreeSpace, freeDiskSpaceByNodes);
        Map<String, String> notEligibleNodes = getNotEligibleNodes(freeDiskSpaceByNodes, requiredFreeSpace,
                stack.getNotTerminatedAndNotZombieGatewayInstanceMetadata());
        if (!notEligibleNodes.isEmpty()) {
            throw new UpgradeValidationFailedException(String.format(
                    "There is not enough free space on the nodes to perform upgrade operation. The required and the available free space by nodes: %s",
                    formatValidationFailureMessage(notEligibleNodes, freeDiskSpaceByNodes)));
        }
    }

    private Map<String, Double> getFreeDiskSpaceByNodes(Stack stack) {
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
        Set<Node> nodes = stackUtil.collectNodesWithDiskData(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        OrchestratorRunParams runParams = new OrchestratorRunParams(nodes, gatewayConfigs,
                DISK_FREE_SPACE_COMMAND, "Failed to get free disk space on hosts.");
        Map<String, String> result = hostOrchestrator.runShellCommandOnNodes(runParams);
        List<String> failedNodes = new ArrayList<>();
        Map<String, Double> freeDiskSpaceByNodes = new HashMap<>();
        for (Map.Entry<String, String> freeDiskSpaceByNode : result.entrySet()) {
            try {
                freeDiskSpaceByNodes.put(freeDiskSpaceByNode.getKey(), Double.parseDouble(freeDiskSpaceByNode.getValue()));
            } catch (NumberFormatException e) {
                failedNodes.add(freeDiskSpaceByNode.getKey());
            }
        }

        verifyFreeDiskSpaceCheckResultAndInstanceState(stack.getId(), failedNodes);
        return freeDiskSpaceByNodes;
    }

    private void verifyFreeDiskSpaceCheckResultAndInstanceState(final Long stackId, final List<String> failedNodes) {
        List<String> stoppedInstances = instanceMetaDataService.getAllNotTerminatedInstanceMetadataViewsByStackId(stackId)
                .stream()
                .filter(metadata -> metadata.getInstanceStatus() == STOPPED)
                .map(InstanceMetadataView::getInstanceId)
                .toList();
        final String msgBase = "Failed to get free disk space from nodes";
        if (!stoppedInstances.isEmpty() & failedNodes.isEmpty()) {
            throw new UpgradeValidationFailedException(String.format("%s due to their stopped state: %s. " +
                    "Please start these instances and retry this operation.", msgBase, stoppedInstances));
        } else if (stoppedInstances.isEmpty() && !failedNodes.isEmpty()) {
            throw new UpgradeValidationFailedException(String.format("%s: %s", msgBase, failedNodes));
        } else if (!stoppedInstances.isEmpty()) {
            throw new UpgradeValidationFailedException(String.format("%s because the following nodes are stopped: %s" +
                    ", and the following ones are in bad condition: %s", msgBase, stoppedInstances, failedNodes));
        }
    }

    private Map<String, String> getNotEligibleNodes(Map<String, Double> freeDiskSpaceByNodes, long parcelSize, List<InstanceMetadataView> gatewayInstances) {
        Map<String, Long> nodesByRequiredFreeSpace = getNodesByRequiredFreeSpace(freeDiskSpaceByNodes, parcelSize, gatewayInstances);
        return nodesByRequiredFreeSpace.entrySet().stream()
                .filter(node -> node.getValue() > freeDiskSpaceByNodes.get(node.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, node -> formatDiskSpace(node.getValue())));
    }

    private Map<String, Long> getNodesByRequiredFreeSpace(Map<String, Double> freeDiskSpaceByNodes, long parcelSize,
            List<InstanceMetadataView> gatewayInstances) {
        return freeDiskSpaceByNodes.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, map -> getRequiredFreeSpace(parcelSize, gatewayInstances, map.getKey())));
    }

    private long getRequiredFreeSpace(long parcelSize, List<InstanceMetadataView> gatewayInstances, String hostname) {
        return (long) (parcelSize * (isGatewayInstance(hostname, gatewayInstances) ? GATEWAY_NODE_PARCEL_SIZE_MULTIPLIER : PARCEL_SIZE_MULTIPLIER));
    }

    private boolean isGatewayInstance(String hostname, List<InstanceMetadataView> gatewayInstances) {
        return gatewayInstances.stream()
                .anyMatch(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null && instanceMetaData.getDiscoveryFQDN().equals(hostname));
    }

    private String formatDiskSpace(double diskSpace) {
        return diskSpace >= GB_IN_KB ? new DecimalFormat("#.#").format(diskSpace / (double) GB_IN_KB) + " GB"
                :  new DecimalFormat("#").format(diskSpace / DIVIDER_TO_MB) + " MB";
    }

    private String formatValidationFailureMessage(Map<String, String> requiredDiskSpaceByNodes, Map<String, Double> freeDiskSpaceByNodes) {
        return requiredDiskSpaceByNodes.entrySet().stream()
                .map(map ->
                        map.getKey()
                                .concat(": required free space is: ")
                                .concat(map.getValue())
                                .concat(" and the available free space is: ")
                                .concat(formatDiskSpace(freeDiskSpaceByNodes.get(map.getKey()))))
                .collect(Collectors.joining(", "));
    }
}
