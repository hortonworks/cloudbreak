package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class AzureTemplateParametersToParametersConverter extends AbstractConversionServiceAwareConverter<AzureParameters, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(AzureParameters source) {
        Map<String, Object> parameters = new HashMap<>();
        if (source.getPrivateId() != null) {
            parameters.put("privateId", source.getPrivateId());
        }
        return parameters;
    }
}
