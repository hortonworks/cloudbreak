package com.sequenceiq.redbeams.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.redbeams.service.uuid.UuidGeneratorService;
import com.sequenceiq.redbeams.service.uuid.UuidService;

public class UuidGeneratorServiceTest {

    private static final String UUID_STRING = "abcdefgh-ijkl-1234-5678-mnopqrstuxyv";

    private static final String NOT_MATCHING_UUID_STRING = "abc";

    @Mock
    private UuidService uuidService;

    @InjectMocks
    private UuidGeneratorService underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGenerateUuidVariablePart() {
        when(uuidService.randomUuid()).thenReturn(UUID_STRING);

        String password = underTest.uuidVariableParts(30);

        assertEquals("abcdefghijkl234678mnopqrstuxyv", password);
    }

    @Test
    public void testGenerateUuidVariablePartWhenLengthShorter() {
        when(uuidService.randomUuid()).thenReturn(UUID_STRING);

        String password = underTest.uuidVariableParts(15);

        assertEquals("abcdefghijkl234", password);
    }

    @Test
    public void testGenerateUuidVariablePartWhenLengthBelow1() {
        when(uuidService.randomUuid()).thenReturn(UUID_STRING);

        String password = underTest.uuidVariableParts(0);

        assertEquals("", password);
        verify(uuidService, never()).randomUuid();
    }

    @Test
    public void testGenerateUuidVariableWhenUuidPatternNotMatching() {
        when(uuidService.randomUuid()).thenReturn(NOT_MATCHING_UUID_STRING);

        String password = underTest.uuidVariableParts(30);

        assertEquals("abc", password);
    }
}
