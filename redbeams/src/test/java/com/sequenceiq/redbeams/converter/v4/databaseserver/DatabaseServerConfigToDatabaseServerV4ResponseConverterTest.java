package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

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
        server.setName("myserver");
        server.setDescription("mine not yours");
        server.setHost("myserver.db.example.com");
        server.setPort(5432);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setConnectionUserName("root");
        server.setConnectionPassword("cloudera");
        server.setConnectorJarUrl("http://drivers.example.com/postgresql.jar");
        server.setCreationDate(System.currentTimeMillis());
        server.setEnvironmentId("myenvironment");

        when(conversionService.convert(any(), any())).thenReturn(new SecretResponse());

        DatabaseServerV4Response response = converter.convert(server);

        verify(conversionService, times(2)).convert(any(), any());

        assertEquals(server.getId(), response.getId());
        assertEquals(server.getName(), response.getName());
        assertEquals(server.getDescription(), response.getDescription());
        assertEquals(server.getHost(), response.getHost());
        assertEquals(server.getPort(), response.getPort());
        assertEquals(server.getDatabaseVendor().databaseType(), response.getDatabaseVendor());
        assertEquals(server.getDatabaseVendor().displayName(), response.getDatabaseVendorDisplayName());
        assertNotNull(response.getConnectionUserName());
        assertNotNull(response.getConnectionPassword());
        assertEquals(server.getConnectorJarUrl(), response.getConnectorJarUrl());
        assertEquals(server.getCreationDate(), response.getCreationDate());
        assertEquals(server.getEnvironmentId(), response.getEnvironmentId());
    }

}
