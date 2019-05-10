package com.sequenceiq.redbeams.converter.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.redbeams.service.crn.CrnServiceTest;
import com.sequenceiq.secret.model.SecretResponse;

public class DatabaseConfigToDatabaseV4ResponseConverterTest {

    private static final Crn CRN = CrnServiceTest.getValidCrn();

    private static final String CONNECTION_DRIVER = "connectionDriver";

    private static final String CONNECTION_URL = "connectionUrl";

    private static final long CREATION_DATE = 1234L;

    private static final String DESCRIPTION = "Description";

    private static final String TYPE = "type";

    private static final String CONNECTOR_JAR_URL = "connectorJarUrl";

    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String NAME = "name";

    private final DatabaseConfigToDatabaseV4ResponseConverter underTest = spy(new DatabaseConfigToDatabaseV4ResponseConverter());

    private final DatabaseConfig databaseConfig = new DatabaseConfig();

    @Before
    public void setup() {
        ConversionService conversionService = mock(ConversionService.class);
        when(conversionService.convert(any(), any())).thenReturn(new SecretResponse());
        doReturn(conversionService).when(underTest).getConversionService();
    }

    @Test
    public void testConvert() {
        DatabaseConfig databaseConfig = new DatabaseConfig();
        databaseConfig.setName(NAME);
        databaseConfig.setCrn(CRN);
        databaseConfig.setDescription(DESCRIPTION);
        databaseConfig.setCreationDate(CREATION_DATE);
//        databaseConfig.setStatus(ResourceStatus.USER_MANAGED);
        databaseConfig.setConnectionDriver(CONNECTION_DRIVER);
        databaseConfig.setConnectionUserName("userName");
        databaseConfig.setConnectionPassword("password");
        databaseConfig.setConnectionURL(CONNECTION_URL);
        databaseConfig.setDatabaseVendor(DatabaseVendor.MYSQL);
        databaseConfig.setType(TYPE);
        databaseConfig.setConnectorJarUrl(CONNECTOR_JAR_URL);
        databaseConfig.setEnvironmentId(ENVIRONMENT_ID);

        DatabaseV4Response response = underTest.convert(databaseConfig);

        assertEquals(NAME, response.getName());
        assertEquals(CRN.toString(), response.getCrn());
        assertEquals(DESCRIPTION, response.getDescription());
        assertEquals(CREATION_DATE, response.getCreationDate().longValue());
//        assertEquals(ResourceStatus.USER_MANAGED.toString(), response.ge);
        assertEquals(CONNECTION_DRIVER, response.getConnectionDriver());
        assertNotNull(response.getConnectionPassword());
        assertNotNull(response.getConnectionUserName());
        assertEquals(CONNECTION_URL, response.getConnectionURL());
        assertEquals(DatabaseVendor.MYSQL.name(), response.getDatabaseEngine());
        assertEquals(TYPE, response.getType());
        assertEquals(CONNECTOR_JAR_URL, response.getConnectorJarUrl());
        assertEquals(ENVIRONMENT_ID, response.getEnvironmentId());
    }
}
