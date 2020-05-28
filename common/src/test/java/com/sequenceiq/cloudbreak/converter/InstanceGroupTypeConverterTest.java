package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.InstanceGroupType;

import javax.persistence.AttributeConverter;

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