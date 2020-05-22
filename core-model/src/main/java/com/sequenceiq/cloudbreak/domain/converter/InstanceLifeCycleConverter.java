package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceLifeCycle;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class InstanceLifeCycleConverter extends DefaultEnumConverter<InstanceLifeCycle> {

    @Override
    public InstanceLifeCycle getDefault() {
        return InstanceLifeCycle.NORMAL;
    }
}
