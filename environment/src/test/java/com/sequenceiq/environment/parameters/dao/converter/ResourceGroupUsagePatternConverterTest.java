package com.sequenceiq.environment.parameters.dao.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

public class ResourceGroupUsagePatternConverterTest extends DefaultEnumConverterBaseTest<ResourceGroupUsagePattern> {

    @Override
    public ResourceGroupUsagePattern getDefaultValue() {
        return ResourceGroupUsagePattern.USE_SINGLE;
    }

    @Override
    public AttributeConverter<ResourceGroupUsagePattern, String> getVictim() {
        return new ResourceGroupUsagePatternConverter();
    }
}
