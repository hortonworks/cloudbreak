package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ResourceType;

public class ResourceTypeConverter extends DefaultEnumConverter<ResourceType> {

    @Override
    public ResourceType getDefault() {
        return ResourceType.AWS_INSTANCE;
    }
}
