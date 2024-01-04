package com.sequenceiq.cloudbreak.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.common.api.type.InstanceGroupType;

public class InstanceGroupTypeConverterTest extends DefaultEnumConverterBaseTest<InstanceGroupType> {

    @Override
    public InstanceGroupType getDefaultValue() {
        return InstanceGroupType.CORE;
    }

    @Override
    public AttributeConverter<InstanceGroupType, String> getVictim() {
        return new InstanceGroupTypeConverter();
    }
}
