package com.sequenceiq.redbeams.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class UserGeneratorServiceTest {

    private static final int USERNAME_LENGTH = 10;

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
}
