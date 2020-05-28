package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverter;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;

public class ResourceGroupUsagePatternConverter extends DefaultEnumConverter<ResourceGroupUsagePattern> {

    @Override
    public ResourceGroupUsagePattern getDefault() {
        return ResourceGroupUsagePattern.USE_SINGLE;
    }
}
