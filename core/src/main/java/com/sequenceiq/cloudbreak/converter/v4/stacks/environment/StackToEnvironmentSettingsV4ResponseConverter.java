package com.sequenceiq.cloudbreak.converter.v4.stacks.environment;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.EnvironmentSettingsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.environment.placement.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

public class StackToEnvironmentSettingsV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, EnvironmentSettingsV4Response> {
    @Override
    public EnvironmentSettingsV4Response convert(Stack source) {
        if (source.getEnvironment() != null) {
            EnvironmentSettingsV4Response response = new EnvironmentSettingsV4Response();
            response.setCredentialName(source.getEnvironment().getCredential().getName());
            response.setName(source.getEnvironment().getName());

            PlacementSettingsV4Response placement = new PlacementSettingsV4Response();
            placement.setAvailabilityZone(source.getAvailabilityZone());
            placement.setRegion(source.getRegion());
            return response;
        }
        return null;
    }
}
