package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.LOCALLY_REDUNDANT;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.PREMIUM_LOCALLY_REDUNDANT;
import static com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType.STANDARD_SSD_LRS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.common.api.type.ResourceType.AWS_ROOT_DISK;
import static com.sequenceiq.common.api.type.ResourceType.AZURE_DISK;
import static com.sequenceiq.common.model.AwsDiskType.Gp2;
import static com.sequenceiq.common.model.AwsDiskType.Gp3;
import static com.sequenceiq.common.model.AwsDiskType.Standard;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class RootDiskValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootDiskValidationService.class);

    private static final Map<String, ResourceType> PLATFORM_RESOURCE_TYPE_MAP =
            ImmutableMap.of(
                AWS.name(), AWS_ROOT_DISK,
                AZURE.name(), AZURE_DISK
            );

    private static final Map<String, List<String>> PLATFORM_DISK_TYPE_MAP =
            ImmutableMap.of(
                    AWS.name(),
                        List.of(
                                Gp2.value(),
                                Gp3.value(),
                                Standard.value()
                        ),
                    AZURE.name(),
                        List.of(
                                STANDARD_SSD_LRS.value(),
                                LOCALLY_REDUNDANT.value(),
                                PREMIUM_LOCALLY_REDUNDANT.value()
                        )
            );

    @Inject
    private TemplateService templateService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void validateRootDiskResourcesForGroup(StackDto stack, String group, String volumeType, int size) {
        String platform = stack.getCloudPlatform();
        if (instanceMetaDataService.anyInvalidMetadataForVerticalScaleInGroup(stack.getId(), group)) {
            throw new BadRequestException("Root volume update requires to all instance be not stopped or deleted on provider." +
                    " Please start the nodes or delete them from CDP portal.");
        }
        if (PLATFORM_RESOURCE_TYPE_MAP.containsKey(platform) && checkPlatformVolumeType(volumeType, platform)) {
            InstanceMetadataView pgwInstanceMetadata = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stack.getId()).orElseThrow();
            Integer defaultRootVolumeSize = defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(
                    platform,
                    group.equalsIgnoreCase(pgwInstanceMetadata.getInstanceGroupName())
            );
            checkUpdateRequiredStackTemplate(stack, group, volumeType, size, defaultRootVolumeSize);
        } else {
            throw new BadRequestException("Root Volume Update is not supported for cloud platform: " + stack.getCloudPlatform() + " and volume type: "
                + volumeType);
        }
    }

    public void validateRootDiskAgainstProviderAndUpdateTemplate(StackDto stack, String volumeType, String group, int size) {
        String platform = stack.getCloudPlatform();
        if (PLATFORM_RESOURCE_TYPE_MAP.containsKey(platform) && checkPlatformVolumeType(volumeType, platform)) {
            InstanceMetadataView pgwInstanceMetadata = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stack.getId()).orElseThrow();
            Integer defaultRootVolumeSize = defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(
                    platform,
                    group.equalsIgnoreCase(pgwInstanceMetadata.getInstanceGroupName())
            );
            updateStackTemplate(stack, group, volumeType, size, defaultRootVolumeSize);
        }
    }

    private void checkUpdateRequiredStackTemplate(
            StackDto stackDto,
            String group,
            String volumeType,
            int size,
            Integer defaultRootVolumeSize
    ) throws BadRequestException {
        Template template = getTemplate(stackDto, group);
        if (!updateSizeRequired(size, template, defaultRootVolumeSize)
                && !updateVolumeTypeRequired(volumeType, template)) {
            throw new BadRequestException("No update required.");
        }
    }

    private Template getTemplate(StackDto stackDto, String group) {
        InstanceGroupDto instanceGroupDto = stackDto.getInstanceGroupByInstanceGroupName(group);
        return instanceGroupDto.getInstanceGroup().getTemplate();
    }

    private boolean updateSizeRequired(int updateSize, Template template, Integer defaultRootVolumeSize) {
        if (updateSize != 0 && updateSize < defaultRootVolumeSize) {
            throw new BadRequestException("Requested size for root volume " + updateSize +
                    " should not be lesser than the default root volume size " + defaultRootVolumeSize + ".");
        }
        return updateSize > 0 && updateSize > defaultRootVolumeSize && updateSize != template.getRootVolumeSize();
    }

    private boolean updateVolumeTypeRequired(String updateVolumeType, Template template) {
        return isNotEmpty(updateVolumeType) && !updateVolumeType.equals(defaultIfEmpty(template.getRootVolumeType(), ""));
    }

    private void updateStackTemplate(StackDto stackDto, String group, String volumeType, int size, Integer defaultRootVolumeSize) {
        LOGGER.debug("Updating template for group {} with root update request: type {} size {}", group, volumeType, size);
        Template template = getTemplate(stackDto, group);
        if (updateSizeRequired(size, template, defaultRootVolumeSize)) {
            template.setRootVolumeSize(size);
        }
        if (updateVolumeTypeRequired(volumeType, template)) {
            template.setRootVolumeType(volumeType);
        }
        templateService.savePure(template);
        LOGGER.debug("Updated template after save: {}", template);
    }

    private boolean checkPlatformVolumeType(String volumeType, String platform) {
        String updateVolumeType = defaultIfEmpty(volumeType, "");
        return isEmpty(updateVolumeType) || PLATFORM_DISK_TYPE_MAP.get(platform).contains(updateVolumeType);
    }
}
