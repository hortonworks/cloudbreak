package com.sequenceiq.freeipa.entity.util;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;

public class InstanceMetadataTypeConverterTest extends DefaultEnumConverterBaseTest<InstanceMetadataType> {

    @Override
    public InstanceMetadataType getDefaultValue() {
        return InstanceMetadataType.GATEWAY;
    }

    @Override
    public AttributeConverter<InstanceMetadataType, String> getVictim() {
        return new InstanceMetadataTypeConverter();
    }
}
