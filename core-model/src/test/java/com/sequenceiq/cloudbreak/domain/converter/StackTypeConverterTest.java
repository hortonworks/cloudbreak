package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class StackTypeConverterTest extends DefaultEnumConverterBaseTest<StackType> {

    @Override
    public StackType getDefaultValue() {
        return StackType.DATALAKE;
    }

    @Override
    public AttributeConverter<StackType, String> getVictim() {
        return new StackTypeConverter();
    }
}
