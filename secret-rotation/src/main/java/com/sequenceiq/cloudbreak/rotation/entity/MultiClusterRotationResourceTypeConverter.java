package com.sequenceiq.cloudbreak.rotation.entity;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class MultiClusterRotationResourceTypeConverter extends DefaultEnumConverter<MultiClusterRotationResourceType> {

    @Override
    public MultiClusterRotationResourceType getDefault() {
        return MultiClusterRotationResourceType.CHILD;
    }
}
