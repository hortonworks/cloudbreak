package com.sequenceiq.cloudbreak.domain.converter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.converter.DefaultEnumConverterBaseTest;

import javax.persistence.AttributeConverter;

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