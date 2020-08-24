package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@Component
public class StackToPlacementSettingsV4ResponseConverter extends AbstractConversionServiceAwareConverter<Stack, PlacementSettingsV4Response> {

    @Override
    public PlacementSettingsV4Response convert(Stack source) {
        PlacementSettingsV4Response response = new PlacementSettingsV4Response();
        response.setRegion(source.getRegion());
        return response;
    }
}
