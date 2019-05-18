package com.sequenceiq.environment.environment.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.environment.model.request.EnvironmentV1Request;
import com.sequenceiq.environment.environment.domain.Environment;

@Component
public class EnvironmentV1RequestToEnvironmentConverter extends AbstractConversionServiceAwareConverter<EnvironmentV1Request, Environment> {
    @Override
    public Environment convert(EnvironmentV1Request source) {
        Environment environment = new Environment();
        environment.setName(source.getName());
        environment.setDescription(source.getDescription());
        return environment;
    }
}
