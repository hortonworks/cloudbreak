package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceLifeCycle;

public class InstanceLifeCycleConverter extends DefaultEnumConverter<InstanceLifeCycle> {

    @Override
    public InstanceLifeCycle getDefault() {
        return InstanceLifeCycle.NORMAL;
    }
}
