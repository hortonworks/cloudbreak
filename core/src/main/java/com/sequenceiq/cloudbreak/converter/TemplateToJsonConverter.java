package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
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
        templateJson.setInstanceType(source.getInstanceType());
        templateJson.setVolumeType(source.getVolumeType());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            templateJson.setParameters(attributes.getMap());
        }
        templateJson.setCloudPlatform(source.cloudPlatform());
        templateJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        if (source.getTopology() != null) {
            templateJson.setTopologyId(source.getTopology().getId());
        }
        return templateJson;
    }
}
