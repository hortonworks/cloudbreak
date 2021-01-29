package com.sequenceiq.environment.experience.common;

import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CommonExperienceResponseReaderTest {

    private static final String TEST_TARGET = "someCallDestination";

    private static final Class<Object> GENERAL_TYPE_FOR_TEST = Object.class;

    @Mock
    private Response mockResponse;

    private CommonExperienceResponseReader underTest;

    @BeforeEach
    void setUp() {
        underTest = new CommonExperienceResponseReader();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testReadWhenResponseIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.read(TEST_TARGET, null, GENERAL_TYPE_FOR_TEST));

        assertEquals("Response should not be null!", expectedException.getMessage());
    }

    @ParameterizedTest
    @EnumSource(
            value = Response.Status.Family.class,
            names = "SUCCESSFUL",
            mode = EnumSource.Mode.EXCLUDE
    )
    void testReadWhenResponseStatusIsNotSuccessfulThenEmptyOptionalReturns(Response.Status.Family outcome) {
        mockStatusForMockResponseOutcome(outcome);

        Optional<Object> result = underTest.read(TEST_TARGET, mockResponse, GENERAL_TYPE_FOR_TEST);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(
            value = Response.Status.Family.class,
            names = "SUCCESSFUL",
            mode = EnumSource.Mode.EXCLUDE
    )
    void testReadWhenResponseStatusIsNotSuccessfulThenNoEntityReadHappens(Response.Status.Family outcome) {
        mockStatusForMockResponseOutcome(outcome);

        underTest.read(TEST_TARGET, mockResponse, GENERAL_TYPE_FOR_TEST);

        verify(mockResponse, never()).readEntity(GENERAL_TYPE_FOR_TEST);
    }

    @Test
    void testReadWhenResponseStatusWasSuccessfulAndEntityReadThrowsIllegalStateExceptionThenEmptyResultShouldReturn() {
        mockStatusForMockResponseOutcome(SUCCESSFUL);
        doThrow(IllegalStateException.class).when(mockResponse).readEntity(GENERAL_TYPE_FOR_TEST);

        Optional<Object> result = underTest.read(TEST_TARGET, mockResponse, GENERAL_TYPE_FOR_TEST);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testReadWhenResponseStatusWasSuccessfulAndEntityReadThrowsProcessingExceptionThenEmptyResultShouldReturn() {
        Response.StatusType statusInfo = mock(Response.StatusType.class);
        when(statusInfo.getFamily()).thenReturn(SUCCESSFUL);
        when(mockResponse.getStatusInfo()).thenReturn(statusInfo);
        doThrow(ProcessingException.class).when(mockResponse).readEntity(GENERAL_TYPE_FOR_TEST);

        Optional<Object> result = underTest.read(TEST_TARGET, mockResponse, GENERAL_TYPE_FOR_TEST);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    private void mockStatusForMockResponseOutcome(Response.Status.Family outcome) {
        Response.StatusType statusInfo = mock(Response.StatusType.class);
        when(statusInfo.getFamily()).thenReturn(outcome);
        when(mockResponse.getStatusInfo()).thenReturn(statusInfo);
    }

}