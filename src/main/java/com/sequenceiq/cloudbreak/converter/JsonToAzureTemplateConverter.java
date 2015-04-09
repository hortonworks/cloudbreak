package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;

@Component
public class JsonToAzureTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateJson, AzureTemplate> {

    @Override
    public AzureTemplate convert(TemplateJson source) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setName(source.getName());
        azureTemplate.setDescription(source.getDescription());
        azureTemplate.setName(String.valueOf(source.getName()));
        azureTemplate.setVmType(AzureVmType.valueOf(source.getParameters().get(AzureTemplateParam.VMTYPE.getName()).toString()));
        azureTemplate.setDescription(source.getDescription());
        azureTemplate.setVolumeCount((source.getVolumeCount() == null) ? 0 : source.getVolumeCount());
        azureTemplate.setVolumeSize((source.getVolumeSize() == null) ? 0 : source.getVolumeSize());
        AzureVmType azureVmType = AzureVmType.valueOf(source.getParameters().get(AzureTemplateParam.VMTYPE.getName()).toString());
        if (azureVmType.maxDiskSize() < azureTemplate.getVolumeCount()) {
            throw new BadRequestException(
                    String.format("Azure not support this volumesize on the %s. The max suppported size is: %s",
                            azureVmType.vmType(),
                            azureVmType.maxDiskSize()));
        }
        return azureTemplate;
    }
}
