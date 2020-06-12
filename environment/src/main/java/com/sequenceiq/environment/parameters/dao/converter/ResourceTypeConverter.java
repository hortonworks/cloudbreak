package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.resourcepersister.ResourceType;

public class ResourceTypeConverter extends DefaultEnumConverter<ResourceType> {

    @Override
    public ResourceType getDefault() {
        return ResourceType.AWS_INSTANCE;
    }
}
