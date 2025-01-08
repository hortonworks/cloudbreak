package com.sequenceiq.cloudbreak.converter.v4.databases;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.database.DatabaseV4RequestToRDSConfigConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@RunWith(MockitoJUnitRunner.class)
public class DatabaseV4RequestToRDSConfigConverterTest {

    private static final String NAME = "test";

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @InjectMocks
    private DatabaseV4RequestToRDSConfigConverter underTest;

    @Before
    public void before() {
        when(resourceNameGenerator.generateName(APIResourceType.RDS_CONFIG)).thenReturn(NAME);
    }

    @Test
    public void postgresJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnPostgresVendorProperties() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    public void mysqlJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnMysqlVendorProperties() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.MYSQL);
    }

    @Test
    public void oracleJdbcConverterTestWhenValidOracleJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:oracle:thin:@test.eu-west-1.rds.amazonaws.com:5432:test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.ORACLE11);

    }

    @Test
    public void rdsConfigConverterTestWhenValidMySQLJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.MYSQL);
    }

    @Test
    public void rdsConfigConverterTestWhenValidPostgresJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    public void rdsConfigConverterTestWhenValidPostgresWithSubnameJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql:subname://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    public void testConverterWhenNameIsNullThenShouldReturnGeneratedName() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setName(null);

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        Assert.assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        Assert.assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        Assert.assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        Assert.assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        Assert.assertEquals(NAME, rdsConfig.getName());
        Assert.assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), rdsConfig.getConnectionDriver());
        Assert.assertEquals(DatabaseVendor.POSTGRES, rdsConfig.getDatabaseEngine());
        verify(resourceNameGenerator, times(1)).generateName(any(APIResourceType.class));
    }

    private DatabaseV4Request rdsConfigRequest() {
        DatabaseV4Request rdsConfigRequest = new DatabaseV4Request();
        rdsConfigRequest.setConnectionPassword("password");
        rdsConfigRequest.setConnectionUserName("username");
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");
        rdsConfigRequest.setName("testname");
        rdsConfigRequest.setType("HIVE");
        return rdsConfigRequest;
    }

    private void assertResult(DatabaseV4Request rdsConfigRequest, RDSConfig rdsConfig, DatabaseVendor databaseVendor) {
        Assert.assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        Assert.assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        Assert.assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        Assert.assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        Assert.assertEquals(rdsConfigRequest.getName(), rdsConfig.getName());
        Assert.assertEquals(databaseVendor.connectionDriver(), rdsConfig.getConnectionDriver());
        Assert.assertEquals(databaseVendor, rdsConfig.getDatabaseEngine());
        verify(resourceNameGenerator, times(0)).generateName(any(APIResourceType.class));
    }
}