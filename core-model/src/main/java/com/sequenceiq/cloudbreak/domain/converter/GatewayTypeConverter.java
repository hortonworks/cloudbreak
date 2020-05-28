package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.GatewayType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class GatewayTypeConverter extends DefaultEnumConverter<GatewayType> {

    @Override
    public GatewayType getDefault() {
        return GatewayType.CENTRAL;
    }
}
