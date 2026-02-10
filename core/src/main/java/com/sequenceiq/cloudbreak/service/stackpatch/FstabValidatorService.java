package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;

import java.io.IOException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltService;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class FstabValidatorService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FstabValidatorService.class);

    private static final String WHITESPACE_REGEX = "\\s+";

    private static final Map<String, ResourceType> CLOUD_RESOURCE_TYPE_CONSTANTS = Map.of(
            AZURE.name(), AZURE_VOLUMESET,
            AWS.name(), AWS_VOLUMESET,
            GCP.name(), GCP_ATTACHED_DISKSET
    );

    private static final EnumSet<Status> PATCH_ALLOWED_STATUSES = EnumSet.of(Status.AVAILABLE, Status.NODE_FAILURE);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    @Inject
    private SaltService saltService;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.FSTAB_VALIDATION;
    }

    @Override
    public boolean isAffected(Stack stack) {
        if (!validateStackForPatch(stack)) {
            throw new CloudbreakServiceException("The stack is not in a valid state to start disk stack patch!");
        }
        boolean affected = false;
        LOGGER.info("Checking if fstab validation stack patch should be run for stack: {}, stack id: {}",
                stack.getStack().getName(), stack.getId());
        if (CLOUD_RESOURCE_TYPE_CONSTANTS.containsKey(stack.getCloudPlatform())) {
            List<Resource> volumeSetResources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                    List.of(CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())));
            stack.setResources(new HashSet<>(volumeSetResources));
            if (!volumeSetResources.isEmpty()) {
                try {
                    Map<String, VolumeSetAttributes> instanceIdVolumeSetMap = getFstabFromResources(volumeSetResources);
                    for (String instanceId : instanceIdVolumeSetMap.keySet()) {
                        if (instanceIdVolumeSetMap.get(instanceId) == null || instanceIdVolumeSetMap.get(instanceId).getFstab() == null) {
                            continue;
                        }
                        String normalizedSavedFstab = normalizeFstab(instanceIdVolumeSetMap.get(instanceId).getFstab());
                        long numberDisksMountedInFstab = getMountedVolumesCount(normalizedSavedFstab);
                        int numberDisksStoredInDB = instanceIdVolumeSetMap.get(instanceId).getVolumes().size();
                        if (numberDisksMountedInFstab != numberDisksStoredInDB) {
                            affected = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    LOGGER.warn("Could not get fstab information", e);
                    throw new CloudbreakServiceException(e);
                }
            }
        }
        LOGGER.info("Is fstab validation stack patch started for stack: {}, stack id: {}, affected: {}",
                stack.getStack().getName(), stack.getId(), affected);
        return affected;
    }

    private static long getMountedVolumesCount(String normalizedSavedFstab) {
        if (normalizedSavedFstab == null || normalizedSavedFstab.isEmpty()) {
            return 0L;
        }

        return Arrays.stream(normalizedSavedFstab.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                .map(line -> line.split(WHITESPACE_REGEX))
                .filter(columns -> columns.length >= 2)
                .filter(columns -> columns[1].startsWith("/hadoopfs/fs") || columns[1].startsWith("/dbfs"))
                .count();
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        boolean applied = false;
        try {
            List<Resource> volumeSetResources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                    List.of(CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())));
            stack.setResources(new HashSet<>(volumeSetResources));
            if (!volumeSetResources.isEmpty()) {
                Map<String, String> saltFstabInfo = getFstabInformation(stack);
                Map<String, String> instanceIdFqdnMap = getInstanceIdFqdnMap(stack);
                for (Resource res : volumeSetResources) {
                    String instanceId = res.getInstanceId();
                    VolumeSetAttributes volumeSetAttribute = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class).orElseThrow();
                    String normalizedSavedFstab = volumeSetAttribute.getFstab() != null ? normalizeFstab(volumeSetAttribute.getFstab()) : "";
                    String normalizedFstabFromSalt = normalizeFstab(saltFstabInfo.getOrDefault(instanceIdFqdnMap.get(instanceId), ""));
                    if (normalizedFstabFromSalt == null || normalizedFstabFromSalt.isEmpty()) {
                        LOGGER.info("Could not retrieve FSTAB from instance {} (FQDN: {}). Stored FSTAB: {}",
                                instanceId, instanceIdFqdnMap.get(instanceId), normalizedSavedFstab);
                        applied = false;
                    } else if (!normalizedSavedFstab.equalsIgnoreCase(normalizedFstabFromSalt) && !volumeSetAttribute.getVolumes().isEmpty()) {
                        LOGGER.info("Replacing existing saved fstab:{} with normalized fstab from salt:{}", normalizedSavedFstab, normalizedFstabFromSalt);
                        volumeSetAttribute.setFstab(normalizedFstabFromSalt);
                        res.setAttributes(new Json(volumeSetAttribute));
                        resourceService.save(res);
                        applied = true;
                    } else {
                        LOGGER.info("FSTAB matches for instance {} (FQDN: {}).", instanceId, instanceIdFqdnMap.get(instanceId));
                        applied = true;
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Exception while running fstab patch on stack {}. Exception::", stack.getId(), ex);
            throw new ExistingStackPatchApplyException(String.format("Fstab patch on %s failed: %s", stack.getId(), ex.getMessage()), ex);
        }
        return applied;
    }

    @NotNull
    private static Map<String, String> getInstanceIdFqdnMap(Stack stack) {
        return stack.getInstanceGroupsAsList().stream()
                .flatMap(ig -> ig.getNotDeletedInstanceMetaDataSet().stream())
                .collect(Collectors.toMap(InstanceMetaData::getInstanceId, InstanceMetaData::getDiscoveryFQDN));
    }

    private boolean validateStackForPatch(Stack stack) {
        if (!PATCH_ALLOWED_STATUSES.contains(stack.getStatus())) {
            LOGGER.warn("Attached volume patch is needed for {} stack, but will be skipped, because its status is not in {}. Current status: {}",
                    stack.getName(), PATCH_ALLOWED_STATUSES, stack.getStatus());
            return false;
        }

        GatewayConfig primaryGateway = gatewayConfigService.getPrimaryGatewayConfig(stack);

        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Set<Node> reachableNodes = saltOrchestrator.getResponsiveNodes(allNodes, primaryGateway, true).getReachableNodes();
        if (allNodes.size() != reachableNodes.size()) {
            LOGGER.warn("Attached volume patch is needed for {} stack, but will be skipped, because not all the nodes are reachable.",
                    stack.getName());
            return false;
        }

        return true;
    }

    private Map<String, String> getFstabInformation(Stack stack) {
        GatewayConfig primaryGateway = gatewayConfigService.getPrimaryGatewayConfig(stack);
        Set<Node> nodesWithDiskData = new HashSet<>(stackUtil.collectNodesWithDiskData(stack));
        Target<String> allHosts = new HostList(nodesWithDiskData.stream().map(Node::getHostname).collect(Collectors.toSet()));
        try {
            LOGGER.info("Fetching fstab from instances to validate against CB context - Nodes - " + allHosts);
            Map<String, Map<String, String>> saltFstabInfo =  saltOrchestrator.getFstabInformation(saltService.createSaltConnector(primaryGateway),
                allHosts, nodesWithDiskData);
            return saltFstabInfo.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, entry -> entry.getValue().getOrDefault("fstab", "")));
        } catch (Exception e) {
            LOGGER.warn("Exception while fetching fstab information for nodes - " + allHosts + " :: Exception - " + e);
            throw e;
        }
    }

    private Map<String, VolumeSetAttributes> getFstabFromResources(List<Resource> volumeSetResources) {
        return volumeSetResources.stream()
            .collect(Collectors.toMap(Resource::getInstanceId, r -> {
                try {
                    return r.getAttributes().get(VolumeSetAttributes.class);
                } catch (IOException e) {
                    LOGGER.warn("Exception while parsing attributes for resource - " + r + " :: Exception - " + e);
                    return null;
                }
            }));
    }

    public String normalizeFstab(String fstab) {
        return fstab.lines()
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .map(l -> l.replaceAll(WHITESPACE_REGEX, " "))
                // Remove duplicate lines
                .distinct()
                // Join them back with newlines (cleaner than reduce)
                .collect(Collectors.joining("\n"));
    }
}