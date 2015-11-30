package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.controller.json.TemplateResponse;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class TemplateToJsonConverter extends AbstractConversionServiceAwareConverter<Template, TemplateResponse> {
    @Override
    public TemplateResponse convert(Template source) {
        TemplateResponse templateJson = new TemplateResponse();
        templateJson.setId(source.getId());
        templateJson.setName(source.getName());
        templateJson.setVolumeCount(source.getVolumeCount());
        templateJson.setVolumeSize(source.getVolumeSize());
        templateJson.setPublicInAccount(source.isPublicInAccount());
        Json attributes = source.getAttributes();
        Map<String, Object> parameters = new HashMap<>();
        if (attributes != null) {
            parameters = attributes.getMap();
        }
        parameters.put("instanceType", source.getInstanceType());
        parameters.put("volumeType", source.getVolumeType());
        templateJson.setParameters(parameters);
        templateJson.setCloudPlatform(source.cloudPlatform());
        templateJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        return templateJson;
    }
}
