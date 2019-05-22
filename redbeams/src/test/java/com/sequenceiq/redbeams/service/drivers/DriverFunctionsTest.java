package com.sequenceiq.redbeams.service.drivers;

import static org.assertj.core.api.Assertions.assertThat;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DriverFunctionsTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    private DriverFunctions driverFunctions;

    @Before
    public void setup() {
        driverFunctions = new DriverFunctions();
    }

    @Test
    public void testDriverFunctions() {
        driverFunctions.execWithDatabaseDriver("", DatabaseVendor.POSTGRES,
                d -> assertThat(d.getDelegate() instanceof org.postgresql.Driver).isTrue());
    }

    @Test
    public void testDriverFunctionsLoadFailure() {
        thrown.expect(DriverLoadingException.class);
        driverFunctions.execWithDatabaseDriver("", DatabaseVendor.MYSQL, d -> {
            // Do nothing
        });
    }
}
