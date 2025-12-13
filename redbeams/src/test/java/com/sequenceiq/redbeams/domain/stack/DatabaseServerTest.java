package com.sequenceiq.redbeams.domain.stack;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DatabaseVendor;
import com.sequenceiq.cloudbreak.common.json.Json;

class DatabaseServerTest {

    private static final SecurityGroup SECURITY_GROUP = new SecurityGroup();

    private static final Json ATTRIBUTES = new Json("{}");

    private DatabaseServer server;

    @BeforeEach
    public void setUp() throws Exception {
        server = new DatabaseServer();
    }

    @Test
    void testGettersAndSetters() {
        server.setId(1L);
        assertEquals(1L, server.getId().longValue());

        server.setName("myserver");
        assertEquals("myserver", server.getName());

        server.setDescription("mine not yours");
        assertEquals("mine not yours", server.getDescription());

        server.setInstanceType("db.m3.medium");
        assertEquals("db.m3.medium", server.getInstanceType());

        server.setDatabaseVendor(DatabaseVendor.POSTGRES);
        assertEquals(DatabaseVendor.POSTGRES, server.getDatabaseVendor());

        server.setConnectionDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", server.getConnectionDriver());

        server.setStorageSize(50L);
        assertEquals(50L, server.getStorageSize().longValue());

        server.setRootUserName("root");
        assertEquals("root", server.getRootUserName());

        server.setRootPassword("cloudera");
        assertEquals("cloudera", server.getRootPassword());

        server.setSecurityGroup(SECURITY_GROUP);
        assertEquals(SECURITY_GROUP, server.getSecurityGroup());

        server.setAttributes(ATTRIBUTES);
        assertEquals(ATTRIBUTES, server.getAttributes());
    }

}
