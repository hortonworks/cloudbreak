package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;

public class InstanceGroupTypeConverter extends DefaultEnumConverter<InstanceGroupType> {

    @Override
    public InstanceGroupType getDefault() {
        return InstanceGroupType.MASTER;
    }
}