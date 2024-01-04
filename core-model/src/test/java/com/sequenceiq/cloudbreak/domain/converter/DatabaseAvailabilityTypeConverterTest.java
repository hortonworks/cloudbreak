package com.sequenceiq.cloudbreak.domain.converter;

import jakarta.persistence.AttributeConverter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

public class DatabaseAvailabilityTypeConverterTest extends DefaultEnumConverterBaseTest<DatabaseAvailabilityType> {

    @Override
    public DatabaseAvailabilityType getDefaultValue() {
        return DatabaseAvailabilityType.NONE;
    }

    @Override
    public AttributeConverter<DatabaseAvailabilityType, String> getVictim() {
        return new DatabaseAvailabilityTypeConverter();
    }
}
