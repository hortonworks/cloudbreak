package com.sequenceiq.cloudbreak.converter.v4.stacks.environment;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class StackToEnvironmentSettingsV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, EnvironmentSettingsV4Response> {

    @Override
    public EnvironmentSettingsV4Response convert(Stack source) {
        if (source.getEnvironment() != null) {
            EnvironmentSettingsV4Response response = new EnvironmentSettingsV4Response();
            response.setCredential(getConversionService().convert(source.getEnvironment().getCredential(), CredentialV4Response.class));
            response.setName(source.getEnvironment().getName());

            PlacementSettingsV4Response placement = new PlacementSettingsV4Response();
            placement.setAvailabilityZone(source.getAvailabilityZone());
            placement.setRegion(source.getRegion());
            response.setPlacement(placement);

            return response;
        }
        return null;
    }

}
