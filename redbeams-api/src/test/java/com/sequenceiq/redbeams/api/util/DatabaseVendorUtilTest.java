package com.sequenceiq.redbeams.api.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;

class DatabaseVendorUtilTest {

    /*
     * This set takes into account the fact that some database vendors will
     * never be returned by getVendorByJdbcUrl, due to DatabaseVendor enum
     * definition ordering. When that ordering changes, this set must be
     * updated.
     */
    private static final EnumSet<DatabaseVendor> TO_TEST =
        EnumSet.of(DatabaseVendor.POSTGRES, DatabaseVendor.MYSQL, DatabaseVendor.MSSQL, DatabaseVendor.ORACLE11,
            DatabaseVendor.SQLANYWHERE, DatabaseVendor.EMBEDDED);

    private DatabaseVendorUtil underTest;

    @BeforeEach
    public void setUp() throws Exception {
        underTest = new DatabaseVendorUtil();
    }

    @Test
    void testValidUrls() {
        for (DatabaseVendor databaseVendor : TO_TEST) {
            String jdbcUrl = "jdbc:" + databaseVendor.jdbcUrlDriverId() + ":etc";
            Optional<DatabaseVendor> foundVendor = underTest.getVendorByJdbcUrl(jdbcUrl);
            assertFalse(foundVendor.isEmpty());
            assertEquals(databaseVendor, foundVendor.get(), "Expected " + databaseVendor + " but got " + foundVendor.get());
        }
    }

    @Test
    void testUnrecognizedUrl() {
        assertTrue(underTest.getVendorByJdbcUrl("unrecognized").isEmpty());
    }

}
