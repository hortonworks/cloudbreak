package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ParametersToOpenStackTemplateParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, OpenStackParameters> {

    @Override
    public OpenStackParameters convert(Map<String, Object> source) {
        return new OpenStackParameters();
    }
}
