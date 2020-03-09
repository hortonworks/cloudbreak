package com.sequenceiq.cloudbreak.converter.v4.stacks.database;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;

class DatabaseAvailabilityTypeToDatabaseResponseConverterTest {

    private final DatabaseAvailabilityTypeToDatabaseResponseConverter underTest = new DatabaseAvailabilityTypeToDatabaseResponseConverter();

    @ParameterizedTest
    @EnumSource(DatabaseAvailabilityType.class)
    void convert(DatabaseAvailabilityType source) {
        DatabaseResponse result = underTest.convert(source);
        result.getAvailabilityType().equals(source);
    }
}
