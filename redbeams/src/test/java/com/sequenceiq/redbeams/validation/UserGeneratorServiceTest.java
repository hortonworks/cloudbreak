package com.sequenceiq.redbeams.validation;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.redbeams.service.UUIDGeneratorService;
import com.sequenceiq.redbeams.service.UserGeneratorService;

public class UserGeneratorServiceTest {

    private static final String UUID_STRING = "abcdefgh-ijkl-1234-5678-mnopqrstuxyv";

    private static final String NOT_MATCHING_UUID_STRING = "abc";

    @Mock
    private UUIDGeneratorService uuidGeneratorService;

    @InjectMocks
    private UserGeneratorService underTest;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGeneratePassword() {
        when(uuidGeneratorService.randomUuid()).thenReturn(UUID_STRING);

        String password = underTest.generatePassword();

        assertEquals("abcdefghijkl234678mnopqrstuxyv", password);
    }

    @Test
    public void testGeneratePasswordNotMatching() {
        when(uuidGeneratorService.randomUuid()).thenReturn(NOT_MATCHING_UUID_STRING);

        String password = underTest.generatePassword();

        assertEquals("abc", password);
    }
}
