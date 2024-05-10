package com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;
import com.sequenceiq.redbeams.api.model.common.Status;

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

        response.setCrn("crn:myserver");
        assertEquals("crn:myserver", response.getCrn());

        response.setDatabaseVendorDisplayName("PostgreSQL");
        assertEquals("PostgreSQL", response.getDatabaseVendorDisplayName());

        response.setConnectionDriver("postgresql.jar");
        assertEquals("postgresql.jar", response.getConnectionDriver());

        SecretResponse username = new SecretResponse("engine", "username");
        response.setConnectionUserName(username);
        verifyEqualSecretResponses(username, response.getConnectionUserName());

        SecretResponse password = new SecretResponse("engine", "password");
        response.setConnectionPassword(password);
        verifyEqualSecretResponses(password, response.getConnectionPassword());

        long now = System.currentTimeMillis();
        response.setCreationDate(now);
        assertEquals(now, response.getCreationDate().longValue());

        response.setResourceStatus(ResourceStatus.USER_MANAGED);
        assertEquals(ResourceStatus.USER_MANAGED, response.getResourceStatus());

        response.setStatus(Status.AVAILABLE);
        assertEquals(Status.AVAILABLE, response.getStatus());

        response.setStatusReason("because");
        assertEquals("because", response.getStatusReason());
    }

    private static void verifyEqualSecretResponses(SecretResponse expected, SecretResponse actual) {
        assertEquals(expected.getEnginePath(), actual.getEnginePath());
        assertEquals(expected.getSecretPath(), actual.getSecretPath());
    }
}
