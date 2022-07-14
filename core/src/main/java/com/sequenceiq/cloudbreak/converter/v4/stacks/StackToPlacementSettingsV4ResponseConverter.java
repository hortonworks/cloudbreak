package com.sequenceiq.cloudbreak.converter.v4.stacks;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.PlacementSettingsV4Response;
import com.sequenceiq.cloudbreak.view.StackView;

@Component
public class StackToPlacementSettingsV4ResponseConverter {

    public PlacementSettingsV4Response convert(StackView source) {
        PlacementSettingsV4Response response = new PlacementSettingsV4Response();
        response.setRegion(source.getRegion());
        return response;
    }
}
