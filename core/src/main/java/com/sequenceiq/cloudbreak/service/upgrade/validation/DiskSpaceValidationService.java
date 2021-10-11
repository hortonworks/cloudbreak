package com.sequenceiq.cloudbreak.service.upgrade.validation;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class DiskSpaceValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSpaceValidationService.class);

    private static final long DIVIDER_TO_MB = 1024L;

    private static final long GB_IN_KB = 1048576;

    private static final double GATEWAY_NODE_PARCEL_SIZE_MULTIPLIER = 3.5;

    private static final double PARCEL_SIZE_MULTIPLIER = 2.5;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private HostOrchestrator hostOrchestrator;

    @Inject
    private ResourceService resourceService;

    @Inject
    private ParcelSizeService parcelSizeService;

    public void validateFreeSpaceForUpgrade(Stack stack, StatedImage targetImage) throws CloudbreakException {
        long requiredFreeSpace = parcelSizeService.getRequiredFreeSpace(targetImage, stack);
        Map<String, String> freeDiskSpaceByNodes = getFreeDiskSpaceByNodes(stack);
        LOGGER.debug("Required free space for parcels {} KB. Free space by nodes in KB: {}", requiredFreeSpace, freeDiskSpaceByNodes);
        Map<String, String> notEligibleNodes = getNotEligibleNodes(freeDiskSpaceByNodes, requiredFreeSpace, stack.getNotTerminatedGatewayInstanceMetadata());
        if (!notEligibleNodes.isEmpty()) {
            throw new UpgradeValidationFailedException(String.format(
                    "There is not enough free space on the nodes to perform upgrade operation. The required free space by nodes: %s",
                    formatFreeSpaceByNodes(notEligibleNodes)));
        }
    }

    private Map<String, String> getFreeDiskSpaceByNodes(Stack stack) {
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
        Set<Node> nodes = stackUtil.collectNodesWithDiskData(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        return hostOrchestrator.getFreeDiskSpaceByNodes(nodes, gatewayConfigs);
    }

    private Map<String, String> getNotEligibleNodes(Map<String, String> freeDiskSpaceByNodes, long parcelSize, List<InstanceMetaData> gatewayInstances) {
        Map<String, Long> nodesByRequiredFreeSpace = getNodesByRequiredFreeSpace(freeDiskSpaceByNodes, parcelSize, gatewayInstances);
        return nodesByRequiredFreeSpace.entrySet().stream()
                .filter(node -> node.getValue() > Double.parseDouble(freeDiskSpaceByNodes.get(node.getKey())))
                .collect(Collectors.toMap(Map.Entry::getKey, node -> formatRequiredSpace(node.getValue())));
    }

    private Map<String, Long> getNodesByRequiredFreeSpace(Map<String, String> freeDiskSpaceByNodes, long parcelSize,
            List<InstanceMetaData> gatewayInstances) {
        return freeDiskSpaceByNodes.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, map -> getRequiredFreeSpace(parcelSize, gatewayInstances, map.getKey())));
    }

    private long getRequiredFreeSpace(long parcelSize, List<InstanceMetaData> gatewayInstances, String hostname) {
        return (long) (parcelSize * (isGatewayInstance(hostname, gatewayInstances) ? GATEWAY_NODE_PARCEL_SIZE_MULTIPLIER : PARCEL_SIZE_MULTIPLIER));
    }

    private boolean isGatewayInstance(String hostname, List<InstanceMetaData> gatewayInstances) {
        return gatewayInstances.stream()
                .anyMatch(instanceMetaData -> instanceMetaData.getDiscoveryFQDN() != null && instanceMetaData.getDiscoveryFQDN().equals(hostname));
    }

    private String formatRequiredSpace(double requiredFreeSpace) {
        return requiredFreeSpace >= GB_IN_KB ? new DecimalFormat("#.#").format(requiredFreeSpace / (double) GB_IN_KB) + " GB"
                : (requiredFreeSpace / DIVIDER_TO_MB) + " MB";
    }

    private String formatFreeSpaceByNodes(Map<String, String> notEligibleNodes) {
        return notEligibleNodes.entrySet().stream()
                .map(map -> map.getKey().concat(": ").concat(map.getValue()))
                .collect(Collectors.joining(", "));
    }
}
