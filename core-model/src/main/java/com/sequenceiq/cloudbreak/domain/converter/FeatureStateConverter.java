package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.FeatureState;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class FeatureStateConverter extends DefaultEnumConverter<FeatureState> {

    @Override
    public FeatureState getDefault() {
        return FeatureState.RELEASED;
    }
}
