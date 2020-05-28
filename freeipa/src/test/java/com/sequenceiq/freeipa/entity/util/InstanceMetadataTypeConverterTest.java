package com.sequenceiq.freeipa.entity.util;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;

import javax.persistence.AttributeConverter;

public class InstanceMetadataTypeConverterTest extends DefaultEnumConverterBaseTest<InstanceMetadataType> {

    @Override
    public InstanceMetadataType getDefaultValue() {
        return InstanceMetadataType.CORE;
    }

    @Override
    public AttributeConverter<InstanceMetadataType, String> getVictim() {
        return new InstanceMetadataTypeConverter();
    }
}