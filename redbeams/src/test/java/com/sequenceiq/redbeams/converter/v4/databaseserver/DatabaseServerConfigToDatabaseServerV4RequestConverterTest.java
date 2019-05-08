package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

public class DatabaseServerConfigToDatabaseServerV4RequestConverterTest {

    private DatabaseServerConfigToDatabaseServerV4RequestConverter converter;

    @Before
    public void setUp() {
        converter = new DatabaseServerConfigToDatabaseServerV4RequestConverter();
    }

    @Test
    public void testConversion() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setName("myserver");
        server.setDescription("mine not yours");
        server.setHost("myserver.db.example.com");
        server.setPort(5432);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setConnectionUserName("root");
        server.setConnectionPassword("cloudera");
        server.setConnectorJarUrl("http://drivers.example.com/postgresql.jar");
        server.setEnvironmentId("myenvironment");

        DatabaseServerV4Request request = converter.convert(server);

        assertEquals(server.getName(), request.getName());
        assertEquals(server.getDescription(), request.getDescription());
        assertEquals(server.getHost(), request.getHost());
        assertEquals(server.getPort(), request.getPort());
        assertEquals(server.getDatabaseVendor().databaseType(), request.getDatabaseVendor());
        assertNotEquals(server.getConnectionUserName(), request.getConnectionUserName());
        assertNotEquals(server.getConnectionPassword(), request.getConnectionPassword());
        assertEquals(server.getConnectorJarUrl(), request.getConnectorJarUrl());
        assertEquals(server.getEnvironmentId(), request.getEnvironmentId());
    }

}
