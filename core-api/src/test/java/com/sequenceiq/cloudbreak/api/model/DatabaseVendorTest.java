package com.sequenceiq.cloudbreak.api.model;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.MSSQL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.MYSQL;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE11;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.ORACLE12;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor.POSTGRES;

import java.util.Arrays;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.OracleParameters;

@RunWith(Parameterized.class)
public class DatabaseVendorTest {

    private final DatabaseV4Request rdsConfigRequest;

    private final Optional<DatabaseVendor> expected;

    public DatabaseVendorTest(DatabaseV4Request rdsConfigRequest, DatabaseVendor expected) {
        this.rdsConfigRequest = rdsConfigRequest;
        this.expected = Optional.ofNullable(expected);
    }

    @Parameters(name = "{index}: databaseVendorTest({0})={1}")
    public static Iterable<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {rdsConfigRequest("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/druidricsi"), POSTGRES},
                {rdsConfigRequest("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/druidricsi"), MYSQL},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsi"), ORACLE11},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsi",
                        oracleParameters("11")), ORACLE11},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsi11g",
                        oracleParameters("11g")), ORACLE11},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsioracle11",
                        oracleParameters("oracle11")), ORACLE11},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsioracle11g",
                        oracleParameters("oracle11g")), ORACLE11},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsioracle12c",
                        oracleParameters("oracle12c")), ORACLE12},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsi12c",
                        oracleParameters("12c")), ORACLE12},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsi12",
                        oracleParameters("12")), ORACLE12},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsioracle12",
                        oracleParameters("oracle12")), ORACLE12},
                {rdsConfigRequest("jdbc:oracle://test.eu-west-1.rds.amazonaws.com:5432/druidricsioracle12s",
                        oracleParameters("oracle12s")), null},
                {rdsConfigRequest("jdbc:sqlserver://test.eu-west-1.rds.amazonaws.com:5432/druidricsi"), MSSQL},
                {rdsConfigRequest("jdbc:smalldog://test.eu-west-1.rds.amazonaws.com:5432/druidricsi"), null},
        });
    }

    public static DatabaseV4Request rdsConfigRequest(String jdbcUrl, Optional<OracleParameters> oracleParameters) {
        DatabaseV4Request rdsConfigRequest = new DatabaseV4Request();
        rdsConfigRequest.setConnectionURL(jdbcUrl);
        rdsConfigRequest.setOracle(oracleParameters.orElse(null));
        return rdsConfigRequest;
    }

    public static DatabaseV4Request rdsConfigRequest(String jdbcUrl) {
        return rdsConfigRequest(jdbcUrl, Optional.empty());
    }

    public static Optional<OracleParameters> oracleParameters(String version) {
        OracleParameters oracleParameters = new OracleParameters();
        oracleParameters.setVersion(version);
        return Optional.of(oracleParameters);
    }

    @Test
    public void test() {
        Optional<DatabaseVendor> databaseVendor = DatabaseVendor.getVendorByJdbcUrl(rdsConfigRequest);
        Assert.assertEquals(String.format("DatabaseVendorTest.getVendorByJdbcUrl returned with %s and the expected was %s for testdata %s",
                databaseVendor, expected, rdsConfigRequest.getConnectionURL()), expected, databaseVendor);
    }

}