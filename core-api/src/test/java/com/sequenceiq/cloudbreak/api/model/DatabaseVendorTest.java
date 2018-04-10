package com.sequenceiq.cloudbreak.api.model;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class DatabaseVendorTest {

    private String jdbcUrl;

    private Optional<DatabaseVendor> expected;

    public DatabaseVendorTest(String jdbcUrl, DatabaseVendor expected) {
        this.jdbcUrl = jdbcUrl;
        this.expected = Optional.ofNullable(expected);
    }

    @Parameterized.Parameters(name = "{index}: databaseVendorTest({0})={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/druidricsi", DatabaseVendor.POSTGRES },
                { "jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/druidricsi",      DatabaseVendor.MYSQL },
                { "jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsi",     DatabaseVendor.ORACLE11, },
                { "jdbc:sqlserver://test.eu-west-1.rds.amazonaws.com:5432/druidricsi",  DatabaseVendor.MSSQL, },
                { "jdbc:smalldog://test.eu-west-1.rds.amazonaws.com:5432/druidricsi",   null },
        });
    }

    @Test
    public void test() {
        Optional<DatabaseVendor> databaseVendor = DatabaseVendor.getVendorByJdbcUrl(this.jdbcUrl);
        Assert.assertEquals(String.format("DatabaseVendorTest.getVendorByJdbcUrl returned with %s and the expected was %s for testdata %s",
                            databaseVendor, this.expected, this.jdbcUrl), this.expected, databaseVendor);
    }

}