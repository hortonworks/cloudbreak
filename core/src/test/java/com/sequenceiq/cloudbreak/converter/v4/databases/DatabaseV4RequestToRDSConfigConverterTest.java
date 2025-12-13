package com.sequenceiq.cloudbreak.converter.v4.databases;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.requests.DatabaseV4Request;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.converter.v4.database.DatabaseV4RequestToRDSConfigConverter;
import com.sequenceiq.cloudbreak.domain.RDSConfig;

@ExtendWith(MockitoExtension.class)
class DatabaseV4RequestToRDSConfigConverterTest {

    private static final String NAME = "test";

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @InjectMocks
    private DatabaseV4RequestToRDSConfigConverter underTest;

    @BeforeEach
    void before() {
        lenient().when(resourceNameGenerator.generateName(APIResourceType.RDS_CONFIG)).thenReturn(NAME);
    }

    @Test
    void postgresJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnPostgresVendorProperties() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    void mysqlJdbcConverterTestWhenDatabaseCanBeDetectedThenShouldReturnMysqlVendorProperties() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.MYSQL);
    }

    @Test
    void oracleJdbcConverterTestWhenValidOracleJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:oracle:thin:@test.eu-west-1.rds.amazonaws.com:5432:test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.ORACLE11);

    }

    @Test
    void rdsConfigConverterTestWhenValidMySQLJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:mysql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.MYSQL);
    }

    @Test
    void rdsConfigConverterTestWhenValidPostgresJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    void rdsConfigConverterTestWhenValidPostgresWithSubnameJdbcUrl() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setConnectionURL("jdbc:postgresql:subname://test.eu-west-1.rds.amazonaws.com:5432/test");

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertResult(rdsConfigRequest, rdsConfig, DatabaseVendor.POSTGRES);
    }

    @Test
    void testConverterWhenNameIsNullThenShouldReturnGeneratedName() {
        DatabaseV4Request rdsConfigRequest = rdsConfigRequest();
        rdsConfigRequest.setName(null);

        RDSConfig rdsConfig = underTest.convert(rdsConfigRequest);

        assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        assertEquals(NAME, rdsConfig.getName());
        assertEquals(DatabaseVendor.POSTGRES.connectionDriver(), rdsConfig.getConnectionDriver());
        assertEquals(DatabaseVendor.POSTGRES, rdsConfig.getDatabaseEngine());
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
        assertEquals(rdsConfigRequest.getConnectionPassword(), rdsConfig.getConnectionPassword());
        assertEquals(rdsConfigRequest.getConnectionUserName(), rdsConfig.getConnectionUserName());
        assertEquals(rdsConfigRequest.getConnectionURL(), rdsConfig.getConnectionURL());
        assertEquals(rdsConfigRequest.getType(), rdsConfig.getType());
        assertEquals(rdsConfigRequest.getName(), rdsConfig.getName());
        assertEquals(databaseVendor.connectionDriver(), rdsConfig.getConnectionDriver());
        assertEquals(databaseVendor, rdsConfig.getDatabaseEngine());
        verify(resourceNameGenerator, times(0)).generateName(any(APIResourceType.class));
    }
}