package com.sequenceiq.redbeams.api.endpoint.v4.database.responses;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.ResourceStatus;

class DatabaseV4ResponseTest {

    private DatabaseV4Response response;

    @BeforeEach
    public void setUp() {
        response = new DatabaseV4Response();
    }

    @Test
    void testGettersAndSetters() {
        response.setCrn("crn:mydb");
        assertEquals("crn:mydb", response.getCrn());

        response.setType("hive");
        assertEquals("hive", response.getType());

        long now = System.currentTimeMillis();
        response.setCreationDate(now);
        assertEquals(now, response.getCreationDate().longValue());

        response.setDatabaseEngine("postgres");
        assertEquals("postgres", response.getDatabaseEngine());

        response.setConnectionDriver("postgresql.jar");
        assertEquals("postgresql.jar", response.getConnectionDriver());

        response.setDatabaseEngineDisplayName("PostgreSQL");
        assertEquals("PostgreSQL", response.getDatabaseEngineDisplayName());

        SecretResponse username = new SecretResponse("engine", "username", 1);
        response.setConnectionUserName(username);
        verifyEqualSecretResponses(username, response.getConnectionUserName());

        SecretResponse password = new SecretResponse("engine", "password", 2);
        response.setConnectionPassword(password);
        verifyEqualSecretResponses(password, response.getConnectionPassword());

        response.setResourceStatus(ResourceStatus.USER_MANAGED);
        assertEquals(ResourceStatus.USER_MANAGED, response.getResourceStatus());
    }

    private static void verifyEqualSecretResponses(SecretResponse expected, SecretResponse actual) {
        assertEquals(expected.getEnginePath(), actual.getEnginePath());
        assertEquals(expected.getSecretPath(), actual.getSecretPath());
    }
}
