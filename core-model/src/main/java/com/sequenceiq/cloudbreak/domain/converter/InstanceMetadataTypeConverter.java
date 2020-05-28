package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;

public class InstanceMetadataTypeConverter extends DefaultEnumConverter<InstanceMetadataType> {

    @Override
    public InstanceMetadataType getDefault() {
        return InstanceMetadataType.CORE;
    }
}
