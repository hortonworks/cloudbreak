package com.sequenceiq.cloudbreak.service.stackpatch;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cloud.notification.ResourceNotifier;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.OrchestratorStateParams;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.SaltOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.salt.PartialSaltStateUpdateService;
import com.sequenceiq.cloudbreak.service.salt.SaltStateParamsService;
import com.sequenceiq.cloudbreak.template.VolumeUtils;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class AzureFixAttachedVolumesPatchService extends ExistingStackPatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureFixAttachedVolumesPatchService.class);

    private static final String DEPRECATED_DEVICE_PATH_PREFIX = "/dev/sd";

    private static final Pattern UUID_RESULT_REGEX_PATTERN = Pattern.compile("(.*): UUID=\"([a-z0-9-]+)\"");

    private static final Pattern FSTAB_ATTACHED_DISK_REGEX_PATTERN = Pattern.compile("^UUID=([a-z0-9-]+)\\s+(/hadoopfs/fs[0-9]+|/dbfs).*$");

    private static final int SALT_RETRY = 3;

    private static final EnumSet<Status> PATCH_ALLOWED_STATUSES = EnumSet.of(Status.AVAILABLE, Status.NODE_FAILURE);

    @Inject
    private ResourceService resourceService;

    @Inject
    private ResourceToCloudResourceConverter resourceConverter;

    @Inject
    private CloudConnectorHelper cloudConnectorHelper;

    @Inject
    private SaltOrchestrator saltOrchestrator;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ResourceNotifier resourceNotifier;

    @Inject
    private SaltStateParamsService saltStateParamsService;

    @Inject
    private PartialSaltStateUpdateService partialSaltStateUpdateService;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private StackUpdater stackUpdater;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.AZURE_ATTACHED_VOLUMES_FIX;
    }

    @Override
    public boolean isAffected(Stack stack) {
        boolean affected;
        if (CloudPlatform.AZURE.name().equalsIgnoreCase(stack.cloudPlatform())) {
            setVolumeSetResourcesForStack(stack);
            affected = stack.getResources().stream()
                    .filter(resource -> resource.getResourceType() == ResourceType.AZURE_VOLUMESET)
                    .map(resource -> resourceAttributeUtil.getTypedAttributes(resource, VolumeSetAttributes.class))
                    .flatMap(Optional::stream)
                    .flatMap(volumeSet -> volumeSet.getVolumes().stream())
                    .anyMatch(volume -> volume.getDevice().startsWith(DEPRECATED_DEVICE_PATH_PREFIX));
        } else {
            affected = false;
        }
        if (affected) {
            LOGGER.info("Azure attached volume patch is needed for {} stack", stack.getName());
        } else {
            LOGGER.info("Azure attached volume patch is not needed for {} stack", stack.getName());
        }
        return affected;
    }

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        if (!PATCH_ALLOWED_STATUSES.contains(stack.getStatus())) {
            LOGGER.warn("Azure attached volume patch is needed for {} stack, but will be skipped, because its status is not in {}. Current status: {}",
                    stack.getName(), PATCH_ALLOWED_STATUSES, stack.getStatus());
            return false;
        }

        DetailedStackStatus originalStatus = stack.getDetailedStatus();
        stackUpdater.updateStackStatus(stack.getId(), DetailedStackStatus.UPDATE_ATTACHED_VOLUMES, "Updating attached volumes metadata.");

        List<GatewayConfig> gatewayConfigs = gatewayConfigService.getAllGatewayConfigs(stack);
        GatewayConfig primaryGateway = gatewayConfigService.getPrimaryGatewayConfig(stack);

        Set<Node> allNodes = stackUtil.collectNodes(stack);
        Set<Node> reachableNodes = saltOrchestrator.getResponsiveNodes(allNodes, primaryGateway, true).getReachableNodes();
        if (allNodes.size() != reachableNodes.size()) {
            LOGGER.warn("Azure attached volume patch is needed for {} stack, but will be skipped, because not all the nodes are reachable.", stack.getName());
            return false;
        }

        CloudConnectResources cloudConnectResources = cloudConnectorHelper.getCloudConnectorResources(stack);
        List<CloudResource> originalVolumeSetCloudResources = stack.getDiskResources().stream()
                .map(resourceConverter::convert)
                .toList();
        try {
            Map<String, String> fqdnByInstanceIdMap = allNodes.stream().collect(Collectors.toMap(Node::getInstanceId, Node::getHostname));
            Map<String, Map<String, String>> volumeDeviceMappingByInstance = cloudConnectResources.getCloudConnector().volumeConnector()
                    .getVolumeDeviceMappingByInstance(cloudConnectResources.getAuthenticatedContext(), cloudConnectResources.getCloudStack());
            LOGGER.debug("VolumeId-device mapping by instanceid from provider: {}", volumeDeviceMappingByInstance);
            if (!volumeDeviceMappingByInstance.keySet().equals(fqdnByInstanceIdMap.keySet())) {
                LOGGER.warn("Different instances from providers and from salt. Instances from provider: {}, and from salt: {}",
                        volumeDeviceMappingByInstance.keySet(), fqdnByInstanceIdMap);
                return false;
            }

            partialSaltStateUpdateService.performSaltUpdate(stack.getId(), List.of("disks/patch"));

            Map<String, Map<String, String>> deviceNameUuidMapByHosts =
                    getDeviceNameUuidMapByHosts(stack, volumeDeviceMappingByInstance, primaryGateway, allNodes);
            LOGGER.debug("devicename-uuid mapping by host from salt: {}", deviceNameUuidMapByHosts);
            if (MapUtils.isEmpty(deviceNameUuidMapByHosts) || deviceNameUuidMapByHosts.size() != allNodes.size()) {
                LOGGER.warn("Disk uuids weren't collected from all the nodes, patch is not applicable. Missing nodes: {}",
                        allNodes.stream().map(Node::getHostname).filter(host -> !deviceNameUuidMapByHosts.containsKey(host)).toList());
                return false;
            }

            List<CloudResource> patchedResources = originalVolumeSetCloudResources.stream()
                    .map(vs -> patchVolumeSetResource(vs, deviceNameUuidMapByHosts.get(fqdnByInstanceIdMap.get(vs.getInstanceId())),
                            volumeDeviceMappingByInstance.get(vs.getInstanceId())))
                    .toList();
            storePatchedResourceAndUpdateMountPillar(stack, originalVolumeSetCloudResources, patchedResources, cloudConnectResources, gatewayConfigs);
            LOGGER.info("Azure attached volume patch is successfully applied on {}", stack.getName());
            return true;
        } catch (Exception ex) {
            LOGGER.error("Exception during azure attached volumes patch on {}. Volumeset resources: {}", stack.getName(), originalVolumeSetCloudResources, ex);
            throw new ExistingStackPatchApplyException(String.format("Azure attached volumes patch on %s failed: %s", stack.getName(), ex.getMessage()), ex);
        } finally {
            stackUpdater.updateStackStatus(stack.getId(), originalStatus);
        }
    }

    private void storePatchedResourceAndUpdateMountPillar(Stack stack, List<CloudResource> originalResources, List<CloudResource> patchedResources,
            CloudConnectResources cloudConnectResources, List<GatewayConfig> gatewayConfigs) throws Exception {
        try {
            resourceNotifier.notifyUpdates(patchedResources, cloudConnectResources.getCloudContext());
            setVolumeSetResourcesForStack(stack);

            Set<Node> allNodesWithDiskData = stackUtil.collectNodesWithDiskData(stack);
            saltOrchestrator.updateMountDiskPillar(stack, gatewayConfigs, allNodesWithDiskData, new ClusterDeletionBasedExitCriteriaModel(stack.getId(),
                    stack.getCluster().getId()), stack.getPlatformVariant(), false);
        } catch (Exception ex) {
            LOGGER.info("Exception during storing patched resources. Rolling back resources to the original ones.", ex);
            resourceNotifier.notifyUpdates(originalResources, cloudConnectResources.getCloudContext());
            throw ex;
        }
    }

    private Map<String, Map<String, String>> getDeviceNameUuidMapByHosts(Stack stack, Map<String, Map<String, String>> volumeDeviceMapping,
            GatewayConfig primaryGateway, Set<Node> nodes) throws CloudbreakOrchestratorFailedException {
        Map<String, List<String>> deviceNamesByHost = volumeDeviceMapping.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> new ArrayList<>(entry.getValue().values())));
        OrchestratorStateParams orchestratorStateParams =
                saltStateParamsService.createStateParams(stack, "disks/patch/get-uuids", SALT_RETRY, SALT_RETRY, primaryGateway, nodes);
        orchestratorStateParams.setStateParams(createSaltParams(deviceNamesByHost, nodes));
        List<Map<String, JsonNode>> saltResponse = saltOrchestrator.applyOrchestratorState(orchestratorStateParams);
        return parseSaltUuidResponse(saltResponse);
    }

    private Map<String, Map<String, String>> parseSaltUuidResponse(List<Map<String, JsonNode>> saltResponse) {
        if (CollectionUtils.isEmpty(saltResponse) || saltResponse.size() != 1) {
            return Map.of();
        } else {
            return saltResponse.getFirst().entrySet().stream()
                    .filter(entry -> entry.getValue().isObject())
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> parseUuidJsonSaltResponse(entry.getValue())));
        }
    }

    private Map<String, String> parseUuidJsonSaltResponse(JsonNode uuidJsonResponse) {
        String stdout = uuidJsonResponse.properties().stream()
                .filter(e -> e.getKey().startsWith("cmd_|-get-uuids_|-blkid -s UUID"))
                .findFirst()
                .map(e -> e.getValue().get("changes"))
                .map(changesNode -> changesNode.get("stdout"))
                .map(JsonNode::textValue)
                .orElse("");
        return stdout.lines()
                .map(String::trim)
                .map(UUID_RESULT_REGEX_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(matcher -> convertLunPath(matcher.group(1)), matcher -> matcher.group(2)));
    }

    private CloudResource patchVolumeSetResource(CloudResource originalResource, Map<String, String> uuidByDeviceNameMap,
            Map<String, String> deviceNameByVolumeIdMap) {
        VolumeSetAttributes originalVolumeSet = originalResource.getTypedAttributes(VolumeSetAttributes.class, () -> new VolumeSetAttributes.Builder().build());
        if (CollectionUtils.isEmpty(originalVolumeSet.getVolumes())) {
            LOGGER.debug("No volumes are attached on {} instance, no patch is needed on {} volumeset", originalResource.getName(), originalResource.getName());
            return originalResource;
        }
        if (StringUtils.isBlank(originalVolumeSet.getFstab())) {
            LOGGER.warn("There are attached volumes, but fstab parameter is empty in volumeset {}", originalVolumeSet);
            throw new IllegalArgumentException(String.format("There are attached volumes, but fstab parameter is empty in %s resource",
                    originalResource.getName()));
        }
        Map<String, String> mountPathByUuidMap = createMountPathByUuidMap(originalVolumeSet.getFstab());
        List<VolumeSetAttributes.Volume> patchedVolumes = new ArrayList<>();
        List<String> patchedUuids = new ArrayList<>();
        LOGGER.debug("Patching volumeset resource {}, uuidByDeviceNameMap: {}, deviceNameByVolumeIdMap: {}, mountPathByUuidMap: {}",
                originalResource, uuidByDeviceNameMap, deviceNameByVolumeIdMap, mountPathByUuidMap);
        for (VolumeSetAttributes.Volume volume : originalVolumeSet.getVolumes()) {
            String deviceName = deviceNameByVolumeIdMap.get(volume.getId());
            if (StringUtils.isBlank(deviceName)) {
                LOGGER.warn("No device name found for {} volumeid. deviceNameByVolumeIdMap: {}", volume.getId(), deviceNameByVolumeIdMap);
                throw new NoSuchElementException("Missing device name for " + volume.getId() + " volume");
            }
            String uuid = uuidByDeviceNameMap.get(deviceName);
            if (StringUtils.isBlank(uuid)) {
                LOGGER.warn("No uuid found for {} device. uuidByDeviceNameMap: {}", deviceName, uuidByDeviceNameMap);
                throw new NoSuchElementException("Missing uuid for " + deviceName + " device");
            }
            String mountPath = mountPathByUuidMap.get(uuid);
            if (StringUtils.isBlank(mountPath)) {
                LOGGER.warn("No mountPath found for {} uuid. mountPathByUuidMap: {}", deviceName, mountPathByUuidMap);
                throw new NoSuchElementException("Missing mountPath for " + uuid + " uuid");
            }
            CloudVolumeUsageType usageType = VolumeUtils.DATABASE_VOLUME.equals(mountPath) ? CloudVolumeUsageType.DATABASE : CloudVolumeUsageType.GENERAL;
            VolumeSetAttributes.Volume patchedVolume =
                    new VolumeSetAttributes.Volume(volume.getId(), deviceName, volume.getSize(), volume.getType(), usageType);
            patchedVolume.setCloudVolumeStatus(volume.getCloudVolumeStatus());
            patchedVolumes.add(patchedVolume);
            patchedUuids.add(uuid);
            LOGGER.debug("Volume is patched:{}Original volume: {}{}Patched volume: {}", System.lineSeparator(), volume, System.lineSeparator(), patchedVolume);
        }
        VolumeSetAttributes patchedVolumeSet = new VolumeSetAttributes.Builder()
                .withAvailabilityZone(originalVolumeSet.getAvailabilityZone())
                .withDeleteOnTermination(originalVolumeSet.getDeleteOnTermination())
                .withFstab(originalVolumeSet.getFstab())
                .withVolumes(patchedVolumes)
                .withVolumeSize(originalVolumeSet.getVolumeSize())
                .withVolumeType(originalVolumeSet.getVolumeType())
                .withDiscoveryFQDN(originalVolumeSet.getDiscoveryFQDN())
                .withUuids(String.join(" ", patchedUuids))
                .build();
        CloudResource patchedResource = CloudResource.builder()
                .cloudResource(originalResource)
                .build();
        patchedResource.setTypedAttributes(patchedVolumeSet);
        LOGGER.info("Volumeset resource patched{}Original resource: {}{}Patched resource: {}",
                System.lineSeparator(), originalResource, System.lineSeparator(), patchedResource);
        return patchedResource;
    }

    private Map<String, Object> createSaltParams(Map<String, List<String>> deviceNamesByHost, Set<Node> allNodesWithDiskData) {
        Map<String, Object> saltParams = new HashMap<>();
        Map<String, Object> hostDiskParams = new HashMap<>();
        saltParams.put("disk_patch", hostDiskParams);
        for (Node node : allNodesWithDiskData) {
            hostDiskParams.put(node.getHostname(), Map.of("attached_devices",
                    String.join(" ", deviceNamesByHost.get(node.getInstanceId()))));
        }
        return saltParams;
    }

    private void setVolumeSetResourcesForStack(Stack stack) {
        ResourceType diskResourceType = stack.getDiskResourceType();
        if (diskResourceType != null) {
            stack.setResources(new HashSet<>(resourceService.findAllByResourceStatusAndResourceTypeAndStackId(
                    CommonStatus.CREATED, ResourceType.AZURE_VOLUMESET, stack.getId())));
        } else {
            stack.setResources(Set.of());
        }
    }

    private Map<String, String> createMountPathByUuidMap(String fstab) {
        return fstab.lines()
                .map(String::trim)
                .map(FSTAB_ATTACHED_DISK_REGEX_PATTERN::matcher)
                .filter(Matcher::matches)
                .collect(Collectors.toMap(matcher -> matcher.group(1), matcher -> matcher.group(2), (v1, v2) -> {
                    if (!Objects.equals(v1, v2)) {
                        throw new IllegalArgumentException("Duplicate fstab lines with the same uuid, but different mountpoint: " + v1 + " vs " + v2);
                    }
                    return v1;
                }));
    }

    private String convertLunPath(String lunPathFromSalt) {
        return lunPathFromSalt.replaceFirst("scsi[1-9]", "scsi[1-9]");
    }
}
