package com.sequenceiq.redbeams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.model.AzureDatabaseType;

class UserGeneratorServiceTest {

    private static final int USERNAME_LENGTH = 10;

    private static final String USER_NAME = "userName";

    private static final String DB_HOST_NAME = "dbHostName.database.azure.com";

    private static final String DB_SHORT_HOST_NAME = "dbHostName";

    private UserGeneratorService underTest;

    @BeforeEach
    public void init() {
        underTest = new UserGeneratorService();
    }

    @Test
    void testGenerateUserNameWhenTheLengthOfTheStringMustBeTen() {
        String userName = underTest.generateUserName();

        assertEquals(USERNAME_LENGTH, userName.length());
        assertTrue(userName.matches("[a-z]*"), userName);
    }

    @Test
    void testUpdateUserNameAzureNullDbType() {
        String updatedUserName = underTest.updateUserName(DatabaseServer.builder().build(), USER_NAME, Optional.of(CloudPlatform.AZURE), DB_HOST_NAME);
        assertEquals(USER_NAME + "@" + DB_SHORT_HOST_NAME, updatedUserName);
    }

    @Test
    void testUpdateUserNameAzureSingleServer() {
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withParams(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.SINGLE_SERVER.name()))
                .build();
        String updatedUserName = underTest.updateUserName(databaseServer, USER_NAME, Optional.of(CloudPlatform.AZURE), DB_HOST_NAME);
        assertEquals(USER_NAME + "@" + DB_SHORT_HOST_NAME, updatedUserName);
    }

    @Test
    void testUpdateUserNameAzureFlexibleServer() {
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withParams(Map.of(AzureDatabaseType.AZURE_DATABASE_TYPE_KEY, AzureDatabaseType.FLEXIBLE_SERVER.name()))
                .build();
        String updatedUserName = underTest.updateUserName(databaseServer, USER_NAME, Optional.of(CloudPlatform.AZURE), DB_HOST_NAME);
        assertEquals(USER_NAME, updatedUserName);
    }

    @Test
    void testUpdateUserNameDefault() {
        String updatedUserName = underTest.updateUserName(DatabaseServer.builder().build(), USER_NAME, Optional.of(CloudPlatform.MOCK), DB_HOST_NAME);
        assertEquals(USER_NAME, updatedUserName);
    }

    @Test
    void testUpdateUserNamePlatformUnknown() {
        String updatedUserName = underTest.updateUserName(DatabaseServer.builder().build(), USER_NAME, Optional.empty(), DB_HOST_NAME);
        assertEquals(USER_NAME, updatedUserName);
    }

}
