package com.sequenceiq.cloudbreak.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.common.api.type.AdjustmentType;

public class AdjustmentTypeConverterTest extends DefaultEnumConverterBaseTest<AdjustmentType> {

    @Override
    public AdjustmentType getDefaultValue() {
        return AdjustmentType.EXACT;
    }

    @Override
    public AttributeConverter<AdjustmentType, String> getVictim() {
        return new AdjustmentTypeConverter();
    }
}
