package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupType;

public class InstanceGroupTypeConverterTest extends DefaultEnumConverterBaseTest<InstanceGroupType> {

    @Override
    public InstanceGroupType getDefaultValue() {
        return InstanceGroupType.MASTER;
    }

    @Override
    public AttributeConverter<InstanceGroupType, String> getVictim() {
        return new InstanceGroupTypeConverter();
    }
}
