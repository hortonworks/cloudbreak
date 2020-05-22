package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.resourcepersister.ResourceType;

import javax.persistence.AttributeConverter;

public class ResourceTypeConverterTest extends DefaultEnumConverterBaseTest<ResourceType> {

    @Override
    public ResourceType getDefaultValue() {
        return ResourceType.AWS_INSTANCE;
    }

    @Override
    public AttributeConverter<ResourceType, String> getVictim() {
        return new ResourceTypeConverter();
    }
}