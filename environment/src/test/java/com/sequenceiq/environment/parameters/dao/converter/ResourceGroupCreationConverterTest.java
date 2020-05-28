package com.sequenceiq.environment.parameters.dao.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;

import javax.persistence.AttributeConverter;

public class ResourceGroupCreationConverterTest extends DefaultEnumConverterBaseTest<ResourceGroupCreation> {

    @Override
    public ResourceGroupCreation getDefaultValue() {
        return ResourceGroupCreation.CREATE_NEW;
    }

    @Override
    public AttributeConverter<ResourceGroupCreation, String> getVictim() {
        return new ResourceGroupCreationConverter();
    }
}