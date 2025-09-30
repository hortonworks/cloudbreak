package com.sequenceiq.datalake.service.sdx;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceGroupV4Base;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.InstanceTemplateV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.template.volume.VolumeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.template.volume.VolumeV4Response;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.datalake.configuration.CDPConfigService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupDiskRequest;
import com.sequenceiq.sdx.api.model.SdxInstanceGroupRequest;

@Service
public class SdxInstanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SdxInstanceService.class);

    @Inject
    private CDPConfigService cdpConfigService;

    public Set<String> getInstanceGroupNamesBySdxDetails(SdxClusterShape clusterShape, String runtimeVersion, String cloudPlatform) {
        if (clusterShape == null || StringUtils.isAnyBlank(runtimeVersion, cloudPlatform)) {
            throw new BadRequestException("The following query params needs to be filled for this request: clusterShape, runtimeVersion, cloudPlatform");
        }
        Set<String> result = new HashSet<>();
        StackV4Request stackV4Request = cdpConfigService.getConfigForKey(new CDPConfigKey(
                CloudPlatform.valueOf(cloudPlatform), clusterShape, runtimeVersion, Architecture.X86_64
        ));
        if (stackV4Request != null && CollectionUtils.isNotEmpty(stackV4Request.getInstanceGroups())) {
            result = stackV4Request.getInstanceGroups().stream().map(InstanceGroupV4Base::getName).collect(Collectors.toSet());
        }
        return result;
    }

    public void overrideDefaultInstanceType(StackV4Request defaultTemplate, List<SdxInstanceGroupRequest> customInstanceGroups,
            List<InstanceGroupV4Request> originalInstanceGroups, List<InstanceGroupV4Response> currentInstanceGroups,
            SdxClusterShape sdxClusterShape) {
        if (CollectionUtils.isNotEmpty(customInstanceGroups)) {
            LOGGER.debug("Override default template with custom instance groups from request.");
            customInstanceGroups.forEach(customInstanceGroup -> {
                InstanceGroupV4Request templateInstanceGroup = getTemplateInstanceGroup(defaultTemplate, customInstanceGroup.getName())
                        .orElseThrow(() -> new BadRequestException("Custom instance group is missing from default template: " + customInstanceGroup.getName()));
                overrideInstanceType(templateInstanceGroup, customInstanceGroup.getInstanceType());
            });
        } else if (CollectionUtils.isNotEmpty(originalInstanceGroups) && CollectionUtils.isNotEmpty(currentInstanceGroups)
                && !SdxClusterShape.LIGHT_DUTY.equals(sdxClusterShape)) {
            LOGGER.debug("Override default template with previous instance groups");
            currentInstanceGroups.forEach(currentInstanceGroup -> {
                originalInstanceGroups
                        .stream()
                        .filter(templateGroup -> StringUtils.equals(templateGroup.getName(), currentInstanceGroup.getName()))
                        .findAny()
                        .ifPresent(originalIGroup -> overrideDefaultInstanceTypeFromPreviousDatalake(defaultTemplate, currentInstanceGroup,
                                originalIGroup));
            });
        }
    }

    private Optional<InstanceGroupV4Request> getTemplateInstanceGroup(StackV4Request defaultTemplate, String instanceName) {
        return defaultTemplate
                .getInstanceGroups()
                .stream()
                .filter(templateGroup -> StringUtils.equals(templateGroup.getName(), instanceName))
                .findAny();
    }

    private void overrideDefaultInstanceTypeFromPreviousDatalake(StackV4Request defaultTemplate, InstanceGroupV4Response currentInstanceGroup,
            InstanceGroupV4Request originalInstanceGroup) {
        if (currentInstanceGroup != null && currentInstanceGroup.getTemplate() != null && originalInstanceGroup.getTemplate() != null &&
                !currentInstanceGroup.getTemplate().getInstanceType().equals(originalInstanceGroup.getTemplate().getInstanceType())) {
            getTemplateInstanceGroup(defaultTemplate, currentInstanceGroup.getName())
                    .ifPresent(templateIg -> overrideInstanceType(templateIg, currentInstanceGroup.getTemplate().getInstanceType()));
        }
    }

    private void overrideInstanceType(InstanceGroupV4Request templateGroup, String newInstanceType) {
        InstanceTemplateV4Request instanceTemplate = templateGroup.getTemplate();
        if (instanceTemplate != null && StringUtils.isNoneBlank(newInstanceType)) {
            LOGGER.info("Override instance group {} instance type from {} to {}",
                    templateGroup.getName(), instanceTemplate.getInstanceType(), newInstanceType);
            instanceTemplate.setInstanceType(newInstanceType);
        }
    }

    public void overrideDefaultInstanceStorage(StackV4Request defaultTemplate, List<SdxInstanceGroupDiskRequest> customInstanceGroupDisks,
            List<InstanceGroupV4Response> currentInstanceGroups, SdxClusterShape sdxClusterShape) {
        if (CollectionUtils.isNotEmpty(customInstanceGroupDisks)) {
            LOGGER.debug("Override default template with custom instance groups from request.");
            customInstanceGroupDisks.forEach(customInstanceGroupDisk -> {
                InstanceGroupV4Request templateInstanceGroup =
                        getTemplateInstanceGroup(defaultTemplate, customInstanceGroupDisk.getName())
                                .orElseThrow(() -> new BadRequestException("Custom instance group is missing from default template: "
                                        + customInstanceGroupDisk.getName()));
                overrideInstanceStorage(templateInstanceGroup, customInstanceGroupDisk.getInstanceDiskSize());
            });
        } else if (CollectionUtils.isNotEmpty(currentInstanceGroups) && !SdxClusterShape.LIGHT_DUTY.equals(sdxClusterShape)) {
            LOGGER.debug("Override default template with modified instance groups from previous datalake.");
            currentInstanceGroups.forEach(instanceGroupDisk -> {
                Optional<InstanceGroupV4Request> templateInstanceGroupOp = getTemplateInstanceGroup(defaultTemplate, instanceGroupDisk.getName());
                overrideInstanceStorageWithPreviousDiskSize(instanceGroupDisk, templateInstanceGroupOp);
            });
        }
    }

    private void overrideInstanceStorage(InstanceGroupV4Request templateGroup, Integer storageSize) {
        InstanceTemplateV4Request instanceTemplate = templateGroup.getTemplate();
        if (instanceTemplate != null && storageSize > 0) {
            instanceTemplate
                    .getAttachedVolumes()
                    .forEach(volumeV4Request -> {
                        LOGGER.info("Override instance group {} storage size from {} to {}",
                                templateGroup.getName(), volumeV4Request.getSize(), storageSize);
                        volumeV4Request.setSize(storageSize);
                    });
        }
    }

    private void overrideInstanceStorageWithPreviousDiskSize(InstanceGroupV4Response instanceGroupDisk,
            Optional<InstanceGroupV4Request> templateInstanceGroupOp) {
        if (templateInstanceGroupOp.isPresent()) {
            int previousStorageSize = instanceGroupDisk
                    .getTemplate()
                    .getAttachedVolumes()
                    .stream()
                    .filter(vol -> vol.getSize() != null)
                    .mapToInt(VolumeV4Response::getSize)
                    .max()
                    .orElse(0);
            int templateStorageSize = templateInstanceGroupOp
                    .get()
                    .getTemplate()
                    .getAttachedVolumes()
                    .stream()
                    .filter(vol -> vol.getSize() != null)
                    .mapToInt(VolumeV4Request::getSize)
                    .max()
                    .orElse(0);
            if (previousStorageSize > templateStorageSize) {
                overrideInstanceStorage(templateInstanceGroupOp.get(), previousStorageSize);
            }
        }
    }
}