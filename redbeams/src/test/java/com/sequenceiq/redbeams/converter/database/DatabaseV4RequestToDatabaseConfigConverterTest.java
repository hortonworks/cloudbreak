package com.sequenceiq.redbeams.converter.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.converter.ResourceNameGenerator;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.database.request.DatabaseV4Request;
import com.sequenceiq.redbeams.api.util.DatabaseVendorUtil;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@ExtendWith(MockitoExtension.class)
class DatabaseV4RequestToDatabaseConfigConverterTest {

    private static final String TYPE = "type";

    private static final String NAME = "name";

    private static final String ENVIRONMENT_CRN = "environmentCrn";

    private static final String DESCRIPTION = "description";

    private static final String CONNECTION_URL = "connectionUrl";

    private static final String CONNECTION_PASSWORD = "connectionPassword";

    private static final String CONNECTION_USERNAME = "connectionUsername";

    private static final String CONNECTION_DRIVER = "connectionDriver";

    @Mock
    private ResourceNameGenerator resourceNameGenerator;

    @Mock
    private Clock clock;

    @Mock
    private DatabaseVendorUtil databaseVendorUtil;

    @InjectMocks
    private DatabaseV4RequestToDatabaseConfigConverter underTest;

    @Test
    void testConvert() {
        DatabaseV4Request request = new DatabaseV4Request();
        request.setType(TYPE);
        request.setName(NAME);
        request.setEnvironmentCrn(ENVIRONMENT_CRN);
        request.setDescription(DESCRIPTION);
        request.setConnectionURL(CONNECTION_URL);
        request.setConnectionPassword(CONNECTION_PASSWORD);
        request.setConnectionUserName(CONNECTION_USERNAME);
        request.setConnectionDriver(CONNECTION_DRIVER);
        when(databaseVendorUtil.getVendorByJdbcUrl(CONNECTION_URL)).thenReturn(Optional.of(DatabaseVendor.POSTGRES));

        DatabaseConfig databaseConfig = underTest.convert(request);

        assertEquals(NAME, databaseConfig.getName());
        assertEquals(TYPE, databaseConfig.getType());
        assertEquals(ENVIRONMENT_CRN, databaseConfig.getEnvironmentId());
        assertEquals(DESCRIPTION, databaseConfig.getDescription());
        assertEquals(CONNECTION_URL, databaseConfig.getConnectionURL());
        assertEquals(CONNECTION_PASSWORD, databaseConfig.getConnectionPassword().getRaw());
        assertEquals(CONNECTION_USERNAME, databaseConfig.getConnectionUserName().getRaw());
        assertEquals(CONNECTION_DRIVER, databaseConfig.getConnectionDriver());
        assertEquals(DatabaseVendor.POSTGRES, databaseConfig.getDatabaseVendor());
        assertEquals(ResourceStatus.USER_MANAGED, databaseConfig.getStatus());
        verify(databaseVendorUtil).getVendorByJdbcUrl(CONNECTION_URL);
        verify(resourceNameGenerator, never()).generateName(any());
    }
}