package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.InstanceGroupType;

public class InstanceGroupTypeConverter extends DefaultEnumConverter<InstanceGroupType> {

    @Override
    public InstanceGroupType getDefault() {
        return InstanceGroupType.CORE;
    }
}
