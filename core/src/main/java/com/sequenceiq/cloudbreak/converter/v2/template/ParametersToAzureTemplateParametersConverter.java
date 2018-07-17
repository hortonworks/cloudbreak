package com.sequenceiq.cloudbreak.converter.v2.template;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.AzureParameters;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class ParametersToAzureTemplateParametersConverter extends AbstractConversionServiceAwareConverter<Map<String, Object>, AzureParameters> {

    @Override
    public AzureParameters convert(Map<String, Object> source) {
        AzureParameters azureParameters = new AzureParameters();
        if (source.get("privateId") != null) {
            azureParameters.setPrivateId(source.get("privateId").toString());
        }
        return azureParameters;
    }
}
