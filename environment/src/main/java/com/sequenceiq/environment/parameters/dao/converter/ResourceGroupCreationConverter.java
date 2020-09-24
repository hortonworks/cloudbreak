package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;

public class ResourceGroupCreationConverter extends DefaultEnumConverter<ResourceGroupCreation> {

    @Override
    public ResourceGroupCreation getDefault() {
        return ResourceGroupCreation.USE_EXISTING;
    }
}
