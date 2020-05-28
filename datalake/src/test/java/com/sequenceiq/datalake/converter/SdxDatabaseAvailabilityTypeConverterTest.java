package com.sequenceiq.datalake.converter;

import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

import javax.persistence.AttributeConverter;

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