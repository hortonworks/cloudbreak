package com.sequenceiq.cloudbreak.converter;

import com.sequenceiq.common.api.type.AdjustmentType;

import javax.persistence.AttributeConverter;

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