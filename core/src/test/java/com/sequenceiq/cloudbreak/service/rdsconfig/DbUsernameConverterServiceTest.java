package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.ConnectionNameFormat;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabasePropertiesV4Response;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;

public class DbUsernameConverterServiceTest {

    private static final String AZURE_DB_HOST_DOMAIN = ".database.azure.com";

    private final DbUsernameConverterService dbUsernameConverterService = new DbUsernameConverterService();

    @Test
    public void testWhenToConnectionUsernameOnAzure() {
        String username = dbUsernameConverterService.toConnectionUsername(
                createDatabaseServerV4Response(ConnectionNameFormat.USERNAME_WITH_HOSTNAME), "username");

        assertEquals("username@hostname", username);
    }

    @Test
    public void testWhenToConnectionUsernameNotOnAzure() {
        String username = dbUsernameConverterService.toConnectionUsername(
                createDatabaseServerV4Response(ConnectionNameFormat.USERNAME_ONLY), "username");

        assertEquals("username", username);
    }

    @Test
    public void testWhenToConnectionUsernameOnAzureByHostname() {
        String username = dbUsernameConverterService.toConnectionUsername(
                createDatabaseServerV4ResponseWithoutProps("hostname" + AZURE_DB_HOST_DOMAIN), "username");

        assertEquals("username@hostname", username);
    }

    @Test
    public void testWhenToConnectionUsernameNotOnAzureByHostname() {
        String username = dbUsernameConverterService.toConnectionUsername(
                createDatabaseServerV4ResponseWithoutProps("hostname"), "username");

        assertEquals("username", username);
    }

    @Test
    public void testWhenToDatabaseUsername() {
        String username = dbUsernameConverterService.toDatabaseUsername("username@hostname");

        assertEquals("username", username);
    }

    private DatabaseServerV4Response createDatabaseServerV4ResponseWithoutProps(String hostName) {
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setHost(hostName);
        return databaseServerV4Response;
    }

    private DatabaseServerV4Response createDatabaseServerV4Response(ConnectionNameFormat connectionNameFormat) {
        DatabasePropertiesV4Response databasePropertiesV4Response = new DatabasePropertiesV4Response();
        databasePropertiesV4Response.setConnectionNameFormat(connectionNameFormat);
        DatabaseServerV4Response databaseServerV4Response = new DatabaseServerV4Response();
        databaseServerV4Response.setDatabasePropertiesV4Response(databasePropertiesV4Response);
        databaseServerV4Response.setHost("hostname");
        return databaseServerV4Response;
    }
}
