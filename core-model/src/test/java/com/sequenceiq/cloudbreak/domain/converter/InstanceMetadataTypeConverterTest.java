package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

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