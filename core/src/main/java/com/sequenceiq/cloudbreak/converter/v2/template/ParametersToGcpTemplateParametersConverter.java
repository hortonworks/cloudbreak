package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ParametersToGcpTemplateParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, GcpParameters> {

    @Override
    public GcpParameters convert(Map<String, Object> source) {
        return new GcpParameters();
    }
}
