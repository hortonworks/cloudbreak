package com.sequenceiq.redbeams.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;

public class UserGeneratorServiceTest {

    private static final int USERNAME_LENGTH = 10;

    private static final String USER_NAME = "userName";

    private static final String DB_HOST_NAME = "dbHostName.database.azure.com";

    private static final String DB_SHORT_HOST_NAME = "dbHostName";

    private UserGeneratorService underTest;

    @Before
    public void init() {
        underTest = new UserGeneratorService();
    }

    @Test
    public void testGenerateUserName() {
        String userName = underTest.generateUserName();

        assertEquals(userName.length(), USERNAME_LENGTH);
        assertTrue(userName, userName.matches("[a-z]*"));
    }

    @Test
    public void testUpdateUserNameAzure() {
        String updatedUserName = underTest.updateUserName(USER_NAME, Optional.of(CloudPlatform.AZURE), DB_HOST_NAME);
        assertEquals(USER_NAME + "@" + DB_SHORT_HOST_NAME, updatedUserName);
    }

    @Test
    public void testUpdateUserNameDefault() {
        String updatedUserName = underTest.updateUserName(USER_NAME, Optional.of(CloudPlatform.MOCK), DB_HOST_NAME);
        assertEquals(USER_NAME, updatedUserName);
    }

    @Test
    public void testUpdateUserNamePlatformUnknown() {
        String updatedUserName = underTest.updateUserName(USER_NAME, Optional.empty(), DB_HOST_NAME);
        assertEquals(USER_NAME, updatedUserName);
    }

}
