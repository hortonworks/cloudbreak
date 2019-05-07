package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static org.junit.Assert.assertEquals;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.SecretV4Response;

import org.junit.Before;
import org.junit.Test;

public class DatabaseServerV4ResponseTest {

    private DatabaseServerV4Response response;

    @Before
    public void setUp() {
        response = new DatabaseServerV4Response();
    }

    @Test
    public void testGettersAndSetters() {
        response.setId(1L);
        assertEquals(1L, response.getId().longValue());

        response.setDatabaseVendorDisplayName("PostgreSQL");
        assertEquals("PostgreSQL", response.getDatabaseVendorDisplayName());

        response.setConnectionDriver("postgresql.jar");
        assertEquals("postgresql.jar", response.getConnectionDriver());

        SecretV4Response username = new SecretV4Response("engine", "username");
        response.setConnectionUserName(username);
        verifyEqualSecretV4Responses(username, response.getConnectionUserName());

        SecretV4Response password = new SecretV4Response("engine", "password");
        response.setConnectionPassword(password);
        verifyEqualSecretV4Responses(password, response.getConnectionPassword());

        long now = System.currentTimeMillis();
        response.setCreationDate(now);
        assertEquals(now, response.getCreationDate().longValue());

    }

    private static void verifyEqualSecretV4Responses(SecretV4Response expected, SecretV4Response actual) {
        assertEquals(expected.getEnginePath(), actual.getEnginePath());
        assertEquals(expected.getSecretPath(), actual.getSecretPath());
    }
}
