package com.sequenceiq.environment.parameters.dao.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;

public class ResourceGroupCreationConverterTest extends DefaultEnumConverterBaseTest<ResourceGroupCreation> {

    @Override
    public ResourceGroupCreation getDefaultValue() {
        return ResourceGroupCreation.USE_EXISTING;
    }

    @Override
    public AttributeConverter<ResourceGroupCreation, String> getVictim() {
        return new ResourceGroupCreationConverter();
    }
}