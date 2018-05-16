package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Matchers.any;
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

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@RunWith(MockitoJUnitRunner.class)
public class RDSConfigRequestToRDSConfigConverterTest {

    private static final String NAME = "test";

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @InjectMocks
    private RDSConfigRequestToRDSConfigConverter underTest;

    @Before
    public void before() {
        when(missingResourceNameGenerator.generateName(APIResourceType.RDS_CONFIG)).thenReturn(NAME);
    }

    @Test
    public void postgresJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnPostgresVendorProperties() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    public void mysqlJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnMysqlVendorProperties() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");
        rdsConfigRequest.setConnectorJarUrl("http://anexampleofmysqlconnectorjarurl/connector.jar");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.MYSQL);
    }

    @Test
    public void oracleJdbcConverterTestWhenValidOracleJdbcUrl() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:oracle:thin:@test.eu-west-1.rds.amazonaws.com:5432:test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.ORACLE11);

    }

    @Test
    public void rdsConfigConverterTestWhenValidMySQLJdbcUrl() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.MYSQL);
    }

    @Test
    public void rdsConfigConverterTestWhenValidPostgresJdbcUrl() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    public void rdsConfigConverterTestWhenValidPostgresWithSubnameJdbcUrl() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql:subname://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    public void testConverterWhenNameIsNullThenShouldReturnGeneratedName() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setName(null);

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        Assert.assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        Assert.assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        Assert.assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        Assert.assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        Assert.assertEquals(NAME, rdsConfig.getName());
        Assert.assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), rdsConfig.getConnectionDriver());
        Assert.assertEquals(DatabaseVendor.POSTGRES, rdsConfig.getDatabaseEngine());
        verify(missingResourceNameGenerator, times(1)).generateName(any(APIResourceType.class));
    }

    private RDSConfigRequest rdsConfigRequest() {
        RDSConfigRequest rdsConfigRequest = new RDSConfigRequest();
        rdsConfigRequest.setConnectionPassword("password");
        rdsConfigRequest.setConnectionUserName("username");
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");
        rdsConfigRequest.setName("testname");
        rdsConfigRequest.setType("HIVE");
        return rdsConfigRequest;
    }

    private void assertResult(RDSConfigRequest rdsConfigRequest, RDSConfig rdsConfig, DatabaseVendor databaseVendor) {
        Assert.assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        Assert.assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        Assert.assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        Assert.assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        Assert.assertEquals(rdsConfigRequest.getName(), rdsConfig.getName());
        Assert.assertEquals(databaseVendor.connectionDriver(), rdsConfig.getConnectionDriver());
        Assert.assertEquals(databaseVendor, rdsConfig.getDatabaseEngine());
        verify(missingResourceNameGenerator, times(0)).generateName(any(APIResourceType.class));
    }
}