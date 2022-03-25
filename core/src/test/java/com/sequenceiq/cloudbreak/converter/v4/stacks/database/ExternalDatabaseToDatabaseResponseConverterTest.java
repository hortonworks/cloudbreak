package com.sequenceiq.cloudbreak.converter.v4.stacks.database;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseResponse;

class ExternalDatabaseToDatabaseResponseConverterTest {

    private final ExternalDatabaseToDatabaseResponseConverter underTest = new ExternalDatabaseToDatabaseResponseConverter();

    @ParameterizedTest
    @EnumSource(DatabaseAvailabilityType.class)
    void convert(DatabaseAvailabilityType source) {
        String databaseEngineVersion = "13";
        DatabaseResponse result = underTest.convert(source, databaseEngineVersion);
        assertThat(result.getAvailabilityType()).isEqualTo(source);
        assertThat(result.getDatabaseEngineVersion()).isEqualTo(databaseEngineVersion);
    }
}
