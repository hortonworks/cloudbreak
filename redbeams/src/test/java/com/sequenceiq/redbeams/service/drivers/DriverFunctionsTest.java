package com.sequenceiq.redbeams.service.drivers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

public class DriverFunctionsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private DriverFunctions underTest;

    @Before
    public void setup() {
        underTest = new DriverFunctions();
    }

    @Test
    public void testDriverFunctions() {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setName("name");
        databaseConfig.setDatabaseVendor(DatabaseVendor.POSTGRES);

        underTest.execWithDatabaseDriver(databaseConfig,
                d -> assertThat(d.getDelegate() instanceof org.postgresql.Driver).isTrue());
    }

    @Test
    public void testDriverFunctionsUnsupportedVendor() {
        DatabaseServerConfig databaseServerConfig = new DatabaseServerConfig();
        databaseServerConfig.setName("name");
        databaseServerConfig.setDatabaseVendor(DatabaseVendor.MYSQL);

        thrown.expect(UnsupportedOperationException.class);
        underTest.execWithDatabaseDriver(databaseServerConfig, d -> {
            // Do nothing
        });
    }
}
