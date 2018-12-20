package com.sequenceiq.cloudbreak.converter.v2;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.CustomInstanceType;
import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.cloud.PlatformParametersConsts;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class TemplateV2RequestToTemplateConverter extends AbstractConversionServiceAwareConverter<TemplateV2Request, Template> {

    @Override
    public Template convert(TemplateV2Request source) {
        Template template = new Template();
        template.setVolumeCount(source.getVolumeCount());
        template.setVolumeSize(source.getVolumeSize());
        template.setInstanceType(source.getInstanceType());
        template.setRootVolumeSize(source.getRootVolumeSize());
        template.setVolumeType(source.getVolumeType());
        CustomInstanceType customInstanceType = source.getCustomInstanceType();

        Map<String, Object> parameters = new HashMap<>(source.getParameters());

        if (customInstanceType != null) {
            parameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE_MEMORY, customInstanceType.getMemory());
            parameters.put(PlatformParametersConsts.CUSTOM_INSTANCETYPE_CPUS, customInstanceType.getCpus());
        }
        if (source.getAwsParameters() != null) {
            parameters.putAll(getConversionService().convert(source.getAwsParameters(), Map.class));
        } else if (source.getAzureParameters() != null) {
            parameters.putAll(getConversionService().convert(source.getAzureParameters(), Map.class));
        } else if (source.getGcpParameters() != null) {
            Map convert = getConversionService().convert(source.getGcpParameters(), Map.class);
            template.setSecretAttributes(toJson(convert).getValue());
        } else if (source.getOpenStackParameters() != null) {
            parameters.putAll(getConversionService().convert(source.getOpenStackParameters(), Map.class));
        } else if (source.getYarnParameters() != null) {
            parameters.putAll(getConversionService().convert(source.getYarnParameters(), Map.class));
        }
        if (parameters != null && !parameters.isEmpty()) {
            template.setAttributes(toJson(parameters));
        }
        return template;
    }

    private Json toJson(Map value) {
        try {
            return new Json(value);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid template parameter format, valid JSON expected.");
        }
    }
}
