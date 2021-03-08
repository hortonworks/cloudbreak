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
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.StackUtil;

@Component
public class DiskSpaceValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DiskSpaceValidationService.class);

    private static final long DIVIDER_TO_MB = 1024L;

    private static final long GB_IN_KB = 1048576;

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

    public void validateFreeSpaceForUpgrade(Stack stack, String imageCatalogUrl, String imageCatalogName, String imageId) throws CloudbreakException {
        long requiredFreeSpace = parcelSizeService.getAllParcelSize(imageCatalogUrl, imageCatalogName, imageId, stack);
        Map<String, String> freeDiskSpaceByNodes = getFreeDiskSpaceByNodes(stack);
        LOGGER.debug("Required free space for parcels {} KB. Free space by nodes in KB: {}", requiredFreeSpace, freeDiskSpaceByNodes);
        Set<String> notEligibleNodes = getNotEligibleNodes(freeDiskSpaceByNodes, requiredFreeSpace);
        if (!notEligibleNodes.isEmpty()) {
            throw new UpgradeValidationFailedException(
                    String.format("There is not enough free space on the following nodes to perform upgrade operation: %s. The required free space is: %s",
                            notEligibleNodes, formatRequiredSpace(requiredFreeSpace)));
        }
    }

    private String formatRequiredSpace(long requiredFreeSpace) {
        return requiredFreeSpace >= GB_IN_KB ? new DecimalFormat("#.#").format(requiredFreeSpace / (double) GB_IN_KB) + " GB"
                : (requiredFreeSpace / DIVIDER_TO_MB) + " MB";
    }

    private Map<String, String> getFreeDiskSpaceByNodes(Stack stack) {
        stack.setResources(new HashSet<>(resourceService.getAllByStackId(stack.getId())));
        Set<Node> nodes = stackUtil.collectNodesWithDiskData(stack);
        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        return hostOrchestrator.getFreeDiskSpaceByNodes(nodes, gatewayConfigs);
    }

    private Set<String> getNotEligibleNodes(Map<String, String> freeDiskSpaceByNodes, long requiredFreeSpace) {
        return freeDiskSpaceByNodes.entrySet()
                .stream()
                .filter(node -> Long.parseLong(node.getValue()) < requiredFreeSpace)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
