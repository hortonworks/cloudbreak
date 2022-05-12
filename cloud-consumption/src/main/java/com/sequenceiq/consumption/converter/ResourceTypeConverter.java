package com.sequenceiq.consumption.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.consumption.api.v1.consumption.model.common.ResourceType;

public class ResourceTypeConverter extends DefaultEnumConverter<ResourceType> {

    @Override
    public ResourceType getDefault() {
        return ResourceType.UNKNOWN;
    }
}
