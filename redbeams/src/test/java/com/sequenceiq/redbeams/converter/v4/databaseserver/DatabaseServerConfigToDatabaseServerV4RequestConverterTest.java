package com.sequenceiq.redbeams.converter.v4.databaseserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.requests.DatabaseServerV4Request;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

class DatabaseServerConfigToDatabaseServerV4RequestConverterTest {

    private DatabaseServerConfigToDatabaseServerV4RequestConverter converter;

    @BeforeEach
    public void setUp() {
        converter = new DatabaseServerConfigToDatabaseServerV4RequestConverter();
    }

    @Test
    void testConversion() {
        DatabaseServerConfig server = new DatabaseServerConfig();
        server.setName("myserver");
        server.setDescription("mine not yours");
        server.setHost("myserver.db.example.com");
        server.setPort(5432);
        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        server.setConnectionUserName("root");
        server.setConnectionPassword("cloudera");
        server.setEnvironmentId("myenvironment");

        DatabaseServerV4Request request = converter.convert(server);

        assertEquals(server.getName(), request.getName());
        assertEquals(server.getDescription(), request.getDescription());
        assertEquals(server.getHost(), request.getHost());
        assertEquals(server.getPort(), request.getPort());
        assertEquals(server.getDatabaseVendor().databaseType(), request.getDatabaseVendor());
        assertNotEquals(server.getConnectionUserName(), request.getConnectionUserName());
        assertNotEquals(server.getConnectionPassword(), request.getConnectionPassword());
        assertEquals(server.getEnvironmentId(), request.getEnvironmentCrn());
    }

}
