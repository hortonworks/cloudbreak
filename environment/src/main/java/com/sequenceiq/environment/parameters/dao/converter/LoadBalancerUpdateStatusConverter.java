package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.api.v1.environment.model.base.LoadBalancerUpdateStatus;

public class LoadBalancerUpdateStatusConverter extends DefaultEnumConverter<LoadBalancerUpdateStatus> {

    @Override
    public LoadBalancerUpdateStatus getDefault() {
        return LoadBalancerUpdateStatus.NOT_STARTED;
    }
}
