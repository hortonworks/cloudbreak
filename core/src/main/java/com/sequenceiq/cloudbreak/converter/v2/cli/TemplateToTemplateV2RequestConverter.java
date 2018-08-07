package com.sequenceiq.cloudbreak.converter.v2.cli;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.TemplateV2Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.Template;

@Component
public class TemplateToTemplateV2RequestConverter extends AbstractConversionServiceAwareConverter<Template, TemplateV2Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemplateToTemplateV2RequestConverter.class);

    @Override
    public TemplateV2Request convert(Template source) {
        TemplateV2Request templateV2Request = new TemplateV2Request();

        Map<String, Object> templateParameters = new HashMap<>();
        Map<String, Object> parameters = source.getAttributes().getMap();
        Map<String, Object> secretParameters = source.getSecretAttributes().getMap();
        templateParameters.putAll(parameters);
        templateParameters.putAll(secretParameters);

        templateV2Request.setParameters(templateParameters);
        templateV2Request.setInstanceType(source.getInstanceType());
        templateV2Request.setVolumeCount(source.getVolumeCount());
        templateV2Request.setVolumeSize(source.getVolumeSize());
        templateV2Request.setVolumeType(source.getVolumeType());
        templateV2Request.setRootVolumeSize(source.getRootVolumeSize());
        return templateV2Request;
    }

}
