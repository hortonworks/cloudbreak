package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;

public class InstanceMetadataTypeConverter extends DefaultEnumConverter<InstanceMetadataType> {

    @Override
    public InstanceMetadataType getDefault() {
        return InstanceMetadataType.CORE;
    }
}
