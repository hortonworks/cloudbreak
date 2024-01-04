package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

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
