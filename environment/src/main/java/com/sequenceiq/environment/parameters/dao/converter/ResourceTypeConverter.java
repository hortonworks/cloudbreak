package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.common.api.type.ResourceType;

public class ResourceTypeConverter extends DefaultEnumConverter<ResourceType> {

    @Override
    public ResourceType getDefault() {
        return ResourceType.AZURE_PRIVATE_DNS_ZONE;
    }
}
