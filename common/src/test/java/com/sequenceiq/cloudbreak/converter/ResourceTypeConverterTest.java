package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.ResourceType;

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