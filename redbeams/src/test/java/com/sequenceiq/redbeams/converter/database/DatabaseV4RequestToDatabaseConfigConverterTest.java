package com.sequenceiq.redbeams.converter.database;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.converter.MissingResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.util.DatabaseVendorUtil;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

public class DatabaseV4RequestToDatabaseConfigConverterTest {

    private static final String TYPE = "type";

    private static final String NAME = "name";

    private static final String ENVIRONMENT_ID = "environmentId";

    private static final String DESCRIPTION = "description";

    private static final String CONNECTION_URL = "connectionUrl";

    private static final String CONNECTION_PASSWORD = "connectionPassword";

    private static final String CONNECTION_USERNAME = "connectionUsername";

    private static final String CONNECTION_DRIVER = "connectionDriver";

    @Mock
    private MissingResourceNameGenerator missingResourceNameGenerator;

    @Mock
    private Clock clock;

    @Mock
    private DatabaseVendorUtil databaseVendorUtil;

    @InjectMocks
    private DatabaseV4RequestToDatabaseConfigConverter underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testConvert() {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setType(TYPE);
        request.setName(NAME);
        request.setEnvironmentCrn(ENVIRONMENT_ID);
        request.setDescription(DESCRIPTION);
        request.setConnectionURL(CONNECTION_URL);
        request.setConnectionPassword(CONNECTION_PASSWORD);
        request.setConnectionUserName(CONNECTION_USERNAME);
        request.setConnectionDriver(CONNECTION_DRIVER);
        when(databaseVendorUtil.getVendorByJdbcUrl(CONNECTION_URL)).thenReturn(Optional.of(DatabaseVendor.POSTGRES));

        DatabaseConfig databaseConfig = underTest.convert(request);

        assertEquals(NAME, databaseConfig.getName());
        assertEquals(TYPE, databaseConfig.getType());
        assertEquals(ENVIRONMENT_ID, databaseConfig.getEnvironmentId());
        assertEquals(DESCRIPTION, databaseConfig.getDescription());
        assertEquals(CONNECTION_URL, databaseConfig.getConnectionURL());
        assertEquals(CONNECTION_PASSWORD, databaseConfig.getConnectionPassword().getRaw());
        assertEquals(CONNECTION_USERNAME, databaseConfig.getConnectionUserName().getRaw());
        assertEquals(CONNECTION_DRIVER, databaseConfig.getConnectionDriver());
        assertEquals(DatabaseVendor.POSTGRES, databaseConfig.getDatabaseVendor());
        assertEquals(ResourceStatus.USER_MANAGED, databaseConfig.getStatus());
        verify(databaseVendorUtil).getVendorByJdbcUrl(CONNECTION_URL);
        verify(missingResourceNameGenerator, never()).generateName(any());
    }
}
