package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.convert.ConversionService;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.TestData;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.domain.stack.DBStackStatus;

public class DatabaseServerConfigToDatabaseServerV4ResponseConverterTest {

    @Mock
    private ConversionService conversionService;

    @InjectMocks
    private DatabaseServerConfigToDatabaseServerV4ResponseConverter converter;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testConversion() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(TestData.getTestCrn("databaseServer", "myserver"));
        server.setName("myserver");
        server.setDescription("mine not yours");
        server.setHost("myserver.db.example.com");
        server.setPort(5432);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setConnectionUserName("root");
        server.setConnectionPassword("cloudera");
        server.setCreationDate(System.currentTimeMillis());
        server.setEnvironmentId("myenvironment");
        server.setResourceStatus(ResourceStatus.SERVICE_MANAGED);
        DBStack dbStack = new DBStack();
        DBStackStatus dbStackStatus = new DBStackStatus();
        dbStackStatus.setStatus(Status.CREATE_IN_PROGRESS);
        dbStackStatus.setStatusReason("Lorem ipsum");
        dbStack.setDBStackStatus(dbStackStatus);
        server.setDbStack(dbStack);
        when(conversionService.convert(any(), any())).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(conversionService, times(2)).convert(any(), any());
        assertEquals(server.getId(), response.getId());
        assertEquals(server.getResourceCrn().toString(), response.getCrn());
        assertEquals(server.getName(), response.getName());
        assertEquals(server.getDescription(), response.getDescription());
        assertEquals(server.getHost(), response.getHost());
        assertEquals(server.getPort(), response.getPort());
        assertEquals(server.getDatabaseVendor().databaseType(), response.getDatabaseVendor());
        assertEquals(server.getDatabaseVendor().displayName(), response.getDatabaseVendorDisplayName());
        assertNotNull(response.getConnectionUserName());
        assertNotNull(response.getConnectionPassword());
        assertEquals(server.getCreationDate(), response.getCreationDate());
        assertEquals(server.getEnvironmentId(), response.getEnvironmentCrn());
        assertEquals(server.getResourceStatus(), response.getResourceStatus());
        assertEquals(server.getDbStack().get().getStatus(), response.getStatus());
        assertEquals(server.getDbStack().get().getStatusReason(), response.getStatusReason());
    }

    @Test
    public void testConversionWhenUserManaged() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setId(1L);
        server.setResourceCrn(TestData.getTestCrn("databaseServer", "myserver"));
        server.setName("myserver");
        server.setDescription("mine not yours");
        server.setHost("myserver.db.example.com");
        server.setPort(5432);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setConnectionUserName("root");
        server.setConnectionPassword("cloudera");
        server.setCreationDate(System.currentTimeMillis());
        server.setEnvironmentId("myenvironment");
        server.setResourceStatus(ResourceStatus.USER_MANAGED);
        when(conversionService.convert(any(), any())).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(conversionService, times(2)).convert(any(), any());
        assertEquals(server.getResourceStatus(), response.getResourceStatus());
        assertNull(response.getStatus());
        assertNull(response.getStatusReason());
    }
}
