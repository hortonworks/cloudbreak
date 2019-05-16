package com.sequenceiq.cloudbreak.converter;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.SpecialParameters;

@Component
public class SpecialParametersToSpecialParametersResponseConverter extends AbstractConversionServiceAwareConverter<SpecialParameters, Map<String, Boolean>> {

    @Override
    public Map<String, Boolean> convert(SpecialParameters source) {
        return source.getSpecialParameters();
    }
}
