package com.sequenceiq.environment.parameters.dao.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.common.api.type.ResourceType;

public class ResourceTypeConverterTest extends DefaultEnumConverterBaseTest<ResourceType> {

    @Override
    public ResourceType getDefaultValue() {
        return ResourceType.AZURE_PRIVATE_DNS_ZONE;
    }

    @Override
    public AttributeConverter<ResourceType, String> getVictim() {
        return new ResourceTypeConverter();
    }
}