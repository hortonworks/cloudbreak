package com.sequenceiq.datalake.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

public class SdxDatabaseAvailabilityTypeConverterTest extends DefaultEnumConverterBaseTest<SdxDatabaseAvailabilityType> {

    @Override
    public SdxDatabaseAvailabilityType getDefaultValue() {
        return SdxDatabaseAvailabilityType.NONE;
    }

    @Override
    public AttributeConverter<SdxDatabaseAvailabilityType, String> getVictim() {
        return new SdxDatabaseAvailabilityTypeConverter();
    }
}
