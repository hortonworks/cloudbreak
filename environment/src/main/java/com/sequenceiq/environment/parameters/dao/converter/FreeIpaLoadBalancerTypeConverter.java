package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;

public class FreeIpaLoadBalancerTypeConverter extends DefaultEnumConverter<FreeIpaLoadBalancerType> {

    @Override
    public FreeIpaLoadBalancerType getDefault() {
        return FreeIpaLoadBalancerType.getDefault();
    }
}
