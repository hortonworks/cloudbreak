package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.SpecialParameters;
import com.sequenceiq.cloudbreak.api.model.SpecialParametersJson;

@Component
public class SpecialParametersToJsonConverter extends AbstractConversionServiceAwareConverter<SpecialParameters, SpecialParametersJson> {

    @Override
    public SpecialParametersJson convert(SpecialParameters source) {
        SpecialParametersJson json = new SpecialParametersJson();
        json.setSpecialParameters(source.getSpecialParameters());
        return json;
    }
}
