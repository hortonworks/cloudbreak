package com.sequenceiq.cloudbreak.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.common.api.type.ResourceType;

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
