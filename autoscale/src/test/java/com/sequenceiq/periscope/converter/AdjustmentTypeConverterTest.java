package com.sequenceiq.periscope.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.periscope.api.model.AdjustmentType;

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
