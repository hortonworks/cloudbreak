package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.LoadBalancerCreation;

public class LoadBalancerCreationConverter extends DefaultEnumConverter<LoadBalancerCreation> {

    @Override
    public LoadBalancerCreation getDefault() {
        return LoadBalancerCreation.ENABLED;
    }
}
