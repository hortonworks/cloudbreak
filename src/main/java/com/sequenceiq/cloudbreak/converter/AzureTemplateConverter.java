package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.TemplateJson;
import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureLocation;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.AzureVmType;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AzureTemplateConverter extends AbstractConverter<TemplateJson, AzureTemplate> {

    @Override
    public TemplateJson convert(AzureTemplate entity) {
        TemplateJson azureTemplateJson = new TemplateJson();
        azureTemplateJson.setName(entity.getName());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setId(entity.getId());
        azureTemplateJson.setDescription(entity.getDescription());
        Map<String, Object> props = new HashMap<>();
        putProperty(props, AzureTemplateParam.LOCATION.getName(), entity.getLocation());
        putProperty(props, AzureTemplateParam.IMAGENAME.getName(), entity.getImageName());
        putProperty(props, AzureTemplateParam.VMTYPE.getName(), entity.getVmType());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setParameters(props);
        azureTemplateJson.setDescription(entity.getDescription() == null ? "" : entity.getDescription());
        azureTemplateJson.setVolumeCount(entity.getVolumeCount());
        azureTemplateJson.setVolumeSize(entity.getVolumeSize());
        azureTemplateJson.setPublicInAccount(entity.isPublicInAccount());
        return azureTemplateJson;
    }

    @Override
    public AzureTemplate convert(TemplateJson json) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setName(json.getName());
        azureTemplate.setDescription(json.getDescription());
        azureTemplate.setImageName(String.valueOf(json.getParameters().get(AzureTemplateParam.IMAGENAME.getName())));
        azureTemplate.setLocation(AzureLocation.valueOf(json.getParameters().get(AzureTemplateParam.LOCATION.getName()).toString()));
        azureTemplate.setName(String.valueOf(json.getName()));
        azureTemplate.setVmType(String.valueOf(json.getParameters().get(AzureTemplateParam.VMTYPE.getName())));
        azureTemplate.setDescription(json.getDescription());
        azureTemplate.setVolumeCount((json.getVolumeCount() == null) ? 0 : json.getVolumeCount());
        azureTemplate.setVolumeSize((json.getVolumeSize() == null) ? 0 : json.getVolumeSize());
        AzureVmType azureVmType = AzureVmType.valueOf(json.getParameters().get(AzureTemplateParam.VMTYPE.getName()).toString());
        if (azureVmType.maxDiskSize() < azureTemplate.getVolumeCount()) {
            throw new BadRequestException(
                    String.format("Azure not support this volumesize on the %s. The max suppported size is: %s",
                            azureVmType.vmType(),
                            azureVmType.maxDiskSize()));
        }
        azureTemplate.setPublicInAccount(json.isPublicInAccount());
        return azureTemplate;
    }

    private void putProperty(Map<String, Object> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
