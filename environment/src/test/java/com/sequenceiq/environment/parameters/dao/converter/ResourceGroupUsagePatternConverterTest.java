package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;

import javax.persistence.AttributeConverter;

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