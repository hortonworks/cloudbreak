package com.sequenceiq.cloudbreak.service.stack.flow;

import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.collect.ImmutableMap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.azure.AzureDiskType;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.InstanceGroupDto;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.DefaultRootVolumeSizeProvider;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.view.InstanceMetadataView;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

@Service
public class RootDiskValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RootDiskValidationService.class);

    private static final Map<String, ResourceType> PLATFORM_RESOURCE_TYPE_MAP = ImmutableMap.of(CloudPlatform.AWS.name(), ResourceType.AWS_ROOT_DISK,
            CloudPlatform.AZURE.name(), ResourceType.AZURE_DISK);

    private static final Map<String, List<String>> PLATFORM_DISK_TYPE_MAP = ImmutableMap.of(CloudPlatform.AWS.name(), List.of(
                    AwsDiskType.Gp2.value(), AwsDiskType.Gp3.value(), AwsDiskType.Standard.value()),
            CloudPlatform.AZURE.name(), List.of(AzureDiskType.STANDARD_SSD_LRS.value(), AzureDiskType.LOCALLY_REDUNDANT.value(),
                    AzureDiskType.PREMIUM_LOCALLY_REDUNDANT.value()));

    @Inject
    private TemplateService templateService;

    @Inject
    private DefaultRootVolumeSizeProvider defaultRootVolumeSizeProvider;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    public void validateRootDiskResourcesForGroupAndUpdateStackTemplate(StackDto stack, DiskUpdateRequest updateRequest) {
        String platform = stack.getCloudPlatform();
        if (PLATFORM_RESOURCE_TYPE_MAP.containsKey(platform) && checkPlatformVolumeType(updateRequest, platform)) {
            InstanceMetadataView pgwInstanceMetadata = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stack.getId()).orElseThrow();
            Integer defaultRootVolumeSize = defaultRootVolumeSizeProvider.getDefaultRootVolumeForPlatform(platform,
                    updateRequest.getGroup().toLowerCase(Locale.ROOT).equals(pgwInstanceMetadata.getInstanceGroupName().toLowerCase(Locale.ROOT)));
            checkUpdateRequiredStackTemplate(stack, updateRequest, defaultRootVolumeSize);
            updateStackTemplate(stack, updateRequest, defaultRootVolumeSize);
        } else {
            throw new BadRequestException("Root Volume Update is not supported for cloud platform: " + stack.getCloudPlatform() + " and volume type: "
                + updateRequest.getVolumeType());
        }
    }

    private void checkUpdateRequiredStackTemplate(StackDto stackDto, DiskUpdateRequest updateRequest,
            Integer defaultRootVolumeSize) throws BadRequestException {
        Template template = getTemplate(stackDto, updateRequest);
        int updateSize = updateRequest.getSize();
        String updateVolumeType = updateRequest.getVolumeType();
        if (!updateSizeRequired(updateSize, template, defaultRootVolumeSize) && !updateVolumeTypeRequired(updateVolumeType, template)) {
            throw new BadRequestException("No update required.");
        }
    }

    private Template getTemplate(StackDto stackDto, DiskUpdateRequest updateRequest) {
        InstanceGroupDto instanceGroupDto = stackDto.getInstanceGroupByInstanceGroupName(updateRequest.getGroup());
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

    private void updateStackTemplate(StackDto stackDto, DiskUpdateRequest updateRequest, Integer defaultRootVolumeSize) {
        LOGGER.debug("Updating template for group {} with root update request: {}", updateRequest.getGroup(), updateRequest);
        Template template = getTemplate(stackDto, updateRequest);
        int updateSize = updateRequest.getSize();
        String updateVolumeType = updateRequest.getVolumeType();
        if (updateSizeRequired(updateSize, template, defaultRootVolumeSize)) {
            template.setRootVolumeSize(updateRequest.getSize());
        }
        if (updateVolumeTypeRequired(updateVolumeType, template)) {
            template.setRootVolumeType(updateRequest.getVolumeType());
        }
        templateService.savePure(template);
        LOGGER.debug("Updated template after save: {}", template);
    }

    private boolean checkPlatformVolumeType(DiskUpdateRequest updateRequest, String platform) {
        String updateVolumeType = defaultIfEmpty(updateRequest.getVolumeType(), "");
        return isEmpty(updateVolumeType) || PLATFORM_DISK_TYPE_MAP.get(platform).contains(updateVolumeType);
    }
}
