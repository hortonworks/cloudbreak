package com.sequenceiq.cloudbreak.service.stackpatch;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static com.sequenceiq.common.api.type.ResourceType.AWS_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_VOLUMESET;
import static com.sequenceiq.common.api.type.ResourceType.GCP_ATTACHED_DISKSET;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.util.ResourceSyncUtil;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class FstabValidatorService extends ExistingStackPatchService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FstabValidatorService.class);

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
    private ResourceSyncUtil resourceSyncUtil;

    @Inject
    private StackStatusAndReachabilityValidatorUtil stackStatusAndReachabilityValidatorUtil;

    @Override
    public StackPatchType getStackPatchType() {
        return StackPatchType.FSTAB_VALIDATION;
    }

    @Override
    public boolean isAffected(Stack stack) {
        if (!stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)) {
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
                        String normalizedSavedFstab = resourceSyncUtil.normalizeFstab(instanceIdVolumeSetMap.get(instanceId).getFstab());
                        long numberDisksMountedInFstab = resourceSyncUtil.getMountedVolumesCount(normalizedSavedFstab);
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

    @Override
    boolean doApply(Stack stack) throws ExistingStackPatchApplyException {
        boolean applied = true;
        try {
            List<Resource> volumeSetResources = resourceService.findAllByStackIdAndResourceTypeIn(stack.getId(),
                    List.of(CLOUD_RESOURCE_TYPE_CONSTANTS.get(stack.getCloudPlatform())));
            stack.setResources(new HashSet<>(volumeSetResources));
            if (!volumeSetResources.isEmpty()) {
                Map<String, String> saltFstabInfo = resourceSyncUtil.getFstabInformation(stack.getId());
                Map<String, String> instanceIdFqdnMap = getInstanceIdFqdnMap(stack);
                for (Resource res : volumeSetResources) {
                    String instanceId = res.getInstanceId();
                    VolumeSetAttributes volumeSetAttribute = resourceAttributeUtil.getTypedAttributes(res, VolumeSetAttributes.class).orElseThrow();
                    String normalizedSavedFstab = volumeSetAttribute.getFstab() != null ? resourceSyncUtil.normalizeFstab(volumeSetAttribute.getFstab()) : "";
                    String normalizedFstabFromSalt = resourceSyncUtil.normalizeFstab(saltFstabInfo.getOrDefault(instanceIdFqdnMap.get(instanceId), ""));
                    if (normalizedFstabFromSalt == null || normalizedFstabFromSalt.isEmpty()) {
                        LOGGER.info("Could not retrieve FSTAB from instance {} (FQDN: {}). Stored FSTAB: {}",
                                instanceId, instanceIdFqdnMap.get(instanceId), normalizedSavedFstab);
                        applied = false;
                    } else if (!normalizedSavedFstab.equalsIgnoreCase(normalizedFstabFromSalt) && !volumeSetAttribute.getVolumes().isEmpty()) {
                        LOGGER.info("Replacing existing saved fstab:{} with normalized fstab from salt:{}", normalizedSavedFstab, normalizedFstabFromSalt);
                        volumeSetAttribute.setFstab(normalizedFstabFromSalt);
                        res.setAttributes(new Json(volumeSetAttribute));
                        resourceService.save(res);
                    } else {
                        LOGGER.info("FSTAB matches for instance {} (FQDN: {}).", instanceId, instanceIdFqdnMap.get(instanceId));
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
}