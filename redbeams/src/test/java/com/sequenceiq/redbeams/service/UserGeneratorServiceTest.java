package com.sequenceiq.redbeams.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.util.AssertionErrors.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.redbeams.service.uuid.UuidGeneratorService;

public class UserGeneratorServiceTest {

    private static final int USERNAME_LENGTH = 10;

    @Mock
    private UuidGeneratorService uuidGeneratorService;

    @InjectMocks
    private UserGeneratorService underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateUserName() {
        String userName = underTest.generateUserName();

        assertEquals(userName.length(), USERNAME_LENGTH);
        assertTrue(userName, userName.matches("[a-z]*"));
    }

    @Test
    public void testGeneratePassword() {
        underTest.generatePassword();

        verify(uuidGeneratorService).uuidVariableParts(30);
    }
}
