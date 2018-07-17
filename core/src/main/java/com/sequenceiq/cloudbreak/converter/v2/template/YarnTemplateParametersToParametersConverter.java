package com.sequenceiq.cloudbreak.converter.v2.template;

import static com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter.PLATFORM_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.GcpParameters;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class YarnTemplateParametersToParametersConverter extends AbstractConversionServiceAwareConverter<GcpParameters, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(GcpParameters source) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PLATFORM_TYPE, CloudConstants.YARN);
        return parameters;
    }
}
