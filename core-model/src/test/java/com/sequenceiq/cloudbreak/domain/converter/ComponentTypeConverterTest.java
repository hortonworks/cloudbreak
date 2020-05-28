package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

public class ComponentTypeConverterTest extends DefaultEnumConverterBaseTest<ComponentType> {

    @Override
    public ComponentType getDefaultValue() {
        return ComponentType.CONTAINER;
    }

    @Override
    public AttributeConverter<ComponentType, String> getVictim() {
        return new ComponentTypeConverter();
    }
}