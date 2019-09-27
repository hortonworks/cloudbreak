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
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.redbeams.domain.DatabaseConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

public class DatabaseConfigToDatabaseV4ResponseConverterTest {

    private static final Crn CRN = TestData.getTestCrn("database", "name");

    private static final String CONNECTION_DRIVER = "connectionDriver";

    private static final String CONNECTION_URL = "connectionUrl";

    private static final long CREATION_DATE = 1234L;

    private static final String DESCRIPTION = "Description";

    private static final String TYPE = "type";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

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
        databaseConfig.setResourceCrn(CRN);
        databaseConfig.setDescription(DESCRIPTION);
        databaseConfig.setCreationDate(CREATION_DATE);
        databaseConfig.setConnectionDriver(CONNECTION_DRIVER);
        databaseConfig.setConnectionUserName("userName");
        databaseConfig.setConnectionPassword("password");
        databaseConfig.setConnectionURL(CONNECTION_URL);
        databaseConfig.setDatabaseVendor(DatabaseVendor.MYSQL);
        databaseConfig.setType(TYPE);
        databaseConfig.setEnvironmentId(ENVIRONMENT_CRN);
        databaseConfig.setStatus(ResourceStatus.SERVICE_MANAGED);

        DatabaseV4Response response = underTest.convert(databaseConfig);

        assertEquals(NAME, response.getName());
        assertEquals(CRN.toString(), response.getCrn());
        assertEquals(DESCRIPTION, response.getDescription());
        assertEquals(CREATION_DATE, response.getCreationDate().longValue());
        assertEquals(CONNECTION_DRIVER, response.getConnectionDriver());
        assertNotNull(response.getConnectionPassword());
        assertNotNull(response.getConnectionUserName());
        assertEquals(CONNECTION_URL, response.getConnectionURL());
        assertEquals(DatabaseVendor.MYSQL.name(), response.getDatabaseEngine());
        assertEquals(TYPE, response.getType());
        assertEquals(ENVIRONMENT_CRN, response.getEnvironmentCrn());
        assertEquals(ResourceStatus.SERVICE_MANAGED, response.getResourceStatus());
    }
}
