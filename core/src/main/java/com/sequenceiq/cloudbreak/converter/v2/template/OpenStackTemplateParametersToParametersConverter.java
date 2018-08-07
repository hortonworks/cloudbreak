package com.sequenceiq.cloudbreak.converter.v2.template;

import static com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter.PLATFORM_TYPE;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackParameters;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class OpenStackTemplateParametersToParametersConverter extends AbstractConversionServiceAwareConverter<OpenStackParameters, Map<String, Object>> {

    @Override
    public Map<String, Object> convert(OpenStackParameters source) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put(PLATFORM_TYPE, CloudConstants.OPENSTACK);
        return parameters;
    }
}
