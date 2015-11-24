package com.sequenceiq.cloudbreak.converter;

import static java.lang.String.format;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.arm.ArmPlatformParameters;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;

@Component
public class JsonToAzureTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateRequest, AzureTemplate> {

    @Override
    public AzureTemplate convert(TemplateRequest source) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setName(source.getName());
        azureTemplate.setDescription(source.getDescription());
        azureTemplate.setName(String.valueOf(source.getName()));
        azureTemplate.setStatus(ResourceStatus.USER_MANAGED);
        azureTemplate.setInstanceType(source.getParameters().get(AzureTemplateParam.VMTYPE.getName()).toString());
        azureTemplate.setDescription(source.getDescription());
        azureTemplate.setVolumeCount((source.getVolumeCount() == null) ? 0 : source.getVolumeCount());
        azureTemplate.setVolumeSize((source.getVolumeSize() == null) ? 0 : source.getVolumeSize());
        String azureVmType = source.getParameters().get(AzureTemplateParam.VMTYPE.getName()).toString();
        int maxDiskCount = ArmPlatformParameters.ArmVmType.getVolumeCountByType(azureVmType);
        if (maxDiskCount < azureTemplate.getVolumeCount()) {
            throw new BadRequestException(format("Max allowed number of disks for vm type: %s is: %s", azureVmType, maxDiskCount));
        }
        return azureTemplate;
    }
}
