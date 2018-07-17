package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.YarnParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ParametersToYarnTemplateParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, YarnParameters> {

    @Override
    public YarnParameters convert(Map<String, Object> source) {
        return new YarnParameters();
    }
}
