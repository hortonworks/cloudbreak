package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.validation.AzureTemplateParam;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;

@Component
public class AzureTemplateToJsonConverter extends AbstractConversionServiceAwareConverter<AzureTemplate, TemplateResponse> {
    @Override
    public TemplateResponse convert(AzureTemplate source) {
        TemplateResponse azureTemplateJson = new TemplateResponse();
        azureTemplateJson.setName(source.getName());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setId(source.getId());
        azureTemplateJson.setDescription(source.getDescription());
        azureTemplateJson.setPublicInAccount(source.isPublicInAccount());
        Map<String, Object> props = new HashMap<>();
        putProperty(props, AzureTemplateParam.VMTYPE.getName(), source.getVmType().name());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setParameters(props);
        azureTemplateJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        azureTemplateJson.setVolumeCount(source.getVolumeCount());
        azureTemplateJson.setVolumeSize(source.getVolumeSize());
        return azureTemplateJson;
    }

    private void putProperty(Map<String, Object> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
