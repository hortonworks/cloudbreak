package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.api.type.LoadBalancerType;

public class LoadBalancerTypeConverter extends DefaultEnumConverter<LoadBalancerType> {

    @Override
    public LoadBalancerType getDefault() {
        return LoadBalancerType.PRIVATE;
    }
}
