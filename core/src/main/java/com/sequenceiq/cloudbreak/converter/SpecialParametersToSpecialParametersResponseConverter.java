package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SpecialParametersToSpecialParametersResponseConverter extends AbstractConversionServiceAwareConverter<SpecialParameters, Map<String, Boolean>> {

    @Override
    public Map<String, Boolean> convert(SpecialParameters source) {
        return source.getSpecialParameters();
    }
}
