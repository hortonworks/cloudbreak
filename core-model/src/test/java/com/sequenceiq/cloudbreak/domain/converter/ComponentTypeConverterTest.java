package com.sequenceiq.cloudbreak.domain.converter;

import javax.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

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