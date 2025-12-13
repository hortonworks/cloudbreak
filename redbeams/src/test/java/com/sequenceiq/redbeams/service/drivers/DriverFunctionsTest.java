package com.sequenceiq.redbeams.service.drivers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

class DriverFunctionsTest {

    private DriverFunctions underTest;

    @BeforeEach
    public void setup() {
        underTest = new DriverFunctions();
    }

    @Test
    void testDriverFunctions() {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setName("name");
        databaseConfig.setDatabaseVendor(DatabaseVendor.POSTGRES);

        underTest.execWithDatabaseDriver(databaseConfig,
                d -> assertThat(d.getDelegate() instanceof org.postgresql.Driver).isTrue());
    }

    @Test
    void testDriverFunctionsUnsupportedVendor() {
        DatabaseServerConfig databaseServerConfig = new DatabaseServerConfig();
        databaseServerConfig.setName("name");
        databaseServerConfig.setDatabaseVendor(DatabaseVendor.MYSQL);

        assertThrows(UnsupportedOperationException.class, () -> underTest.execWithDatabaseDriver(databaseServerConfig, d -> {
            // Do nothing
        }));
    }
}
