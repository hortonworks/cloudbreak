package com.sequenceiq.cloudbreak.converter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.model.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.model.rds.RDSConfigRequest;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.service.MissingResourceNameGenerator;

@RunWith(MockitoJUnitRunner.class)
public class RDSConfigRequestToRDSConfigConverterTest {

    private static final String NAME = "test";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

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

        Assert.assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        Assert.assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        Assert.assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        Assert.assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        Assert.assertEquals(rdsConfigRequest.getName(), rdsConfig.getName());
        Assert.assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), rdsConfig.getConnectionDriver());
        Assert.assertEquals(DatabaseVendor.POSTGRES.name(), rdsConfig.getDatabaseEngine());
        verify(missingResourceNameGenerator, times(0)).generateName(any(APIResourceType.class));
    }

    @Test
    public void mysqlJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnMysqlVendorProperties() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        Assert.assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        Assert.assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        Assert.assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        Assert.assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        Assert.assertEquals(rdsConfigRequest.getName(), rdsConfig.getName());
        Assert.assertEquals(DatabaseVendor.MYSQL.connectionDriver(), rdsConfig.getConnectionDriver());
        Assert.assertEquals(DatabaseVendor.MYSQL.name(), rdsConfig.getDatabaseEngine());
        verify(missingResourceNameGenerator, times(0)).generateName(any(APIResourceType.class));
    }

    @Test
    public void unsupportedJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnBadRequestException() {
        RDSConfigRequest rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:smalldog://test.eu-west-1.rds.amazonaws.com:5432/test");

        thrown.expect(BadRequestException.class);

        underTest.convert(rdsConfigRequest);

        verify(missingResourceNameGenerator, times(0)).generateName(any(APIResourceType.class));
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
        Assert.assertEquals(DatabaseVendor.POSTGRES.name(), rdsConfig.getDatabaseEngine());
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
}