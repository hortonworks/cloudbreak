package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DbUsernameConverterServiceTest {

    private static final String AZURE_DB_HOST_DOMAIN = ".database.azure.com";

    private final DbUsernameConverterService dbUsernameConverterService = new DbUsernameConverterService();

    @Test
    public void testWhenToConnectionUsernameOnAzure() {
        String username = dbUsernameConverterService.toConnectionUsername("hostname" + AZURE_DB_HOST_DOMAIN, "username");

        assertEquals("username@hostname", username);
    }

    @Test
    public void testWhenToConnectionUsernameNotOnAzure() {
        String username = dbUsernameConverterService.toConnectionUsername("hostname.something.com", "username");

        assertEquals("username", username);
    }

    @Test
    public void testWhenToDatabaseUsername() {
        String username = dbUsernameConverterService.toDatabaseUsername("username@hostname");

        assertEquals("username", username);
    }
}
