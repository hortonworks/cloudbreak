package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.json.TemplateRequest;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class JsonToTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateRequest, Template> {

    private static final String INSTANCE_TYPE = "instanceType";
    private static final String VOLUME_TYPE = "volumeType";

    @Override
    public Template convert(TemplateRequest source) {
        Template template = new Template();
        template.setName(source.getName());
        template.setDescription(source.getDescription());
        template.setStatus(ResourceStatus.USER_MANAGED);
        template.setVolumeCount(source.getVolumeCount() == null ? 0 : source.getVolumeCount());
        template.setVolumeSize(source.getVolumeSize() == null ? 0 : source.getVolumeSize());
        Map<String, Object> parameters = source.getParameters();
        template.setInstanceType(String.valueOf(parameters.get(INSTANCE_TYPE)));
        template.setVolumeType(String.valueOf(parameters.get(VOLUME_TYPE)));
        parameters.remove(INSTANCE_TYPE);
        parameters.remove(VOLUME_TYPE);
        template.setCloudPlatform(source.getCloudPlatform());
        if (parameters != null && !parameters.isEmpty()) {
            try {
                template.setAttributes(new Json(parameters));
            } catch (JsonProcessingException e) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        return template;
    }
}
