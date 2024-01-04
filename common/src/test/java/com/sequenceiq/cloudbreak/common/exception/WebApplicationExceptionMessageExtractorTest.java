package com.sequenceiq.cloudbreak.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

class WebApplicationExceptionMessageExtractorTest {

    private WebApplicationExceptionMessageExtractor underTest = new WebApplicationExceptionMessageExtractor();

    @Test
    void testGetErrorMessageWhenExceptionIsNotWebApplicationException() {
        String errorMessage = underTest.getErrorMessage(new RuntimeException("error"));
        assertEquals("error", errorMessage);
    }

    @Test
    void testGetErrorMessageWhenWebApplicationExceptionAndHasNoEntity() {
        String errorMessage = underTest.getErrorMessage(new WebApplicationException("error"));
        assertEquals("error", errorMessage);
    }

    @Test
    void testGetErrorMessageWhenWebApplicationExceptionAndReadEntityFails() {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(Boolean.TRUE);
        when(response.readEntity(eq(String.class))).thenThrow(new RuntimeException("read failed"));
        String errorMessage = underTest.getErrorMessage(new WebApplicationException("error", response));
        assertEquals("error", errorMessage);
    }

    @Test
    void testGetErrorMessageWhenWebApplicationExceptionValidationError() throws JsonProcessingException {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(Boolean.TRUE);
        when(response.readEntity(eq(String.class))).thenReturn(JsonUtil.writeValueAsString(Map.of("validationErrors", "validation error")));
        String errorMessage = underTest.getErrorMessage(new WebApplicationException("error", response));
        assertEquals("Validation error: \"validation error\"", errorMessage);
    }

    @Test
    void testGetErrorMessageWhenWebApplicationExceptionMessage() throws JsonProcessingException {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(Boolean.TRUE);
        when(response.readEntity(eq(String.class))).thenReturn(JsonUtil.writeValueAsString(Map.of("message", "real message")));
        String errorMessage = underTest.getErrorMessage(new WebApplicationException("error", response));
        assertEquals("Error message: \"real message\"", errorMessage);
    }

    @Test
    void testGetErrorMessageWhenWebApplicationExceptionFallbackToErrorResponse() throws JsonProcessingException {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(Boolean.TRUE);
        when(response.readEntity(eq(String.class))).thenReturn(JsonUtil.writeValueAsString(Map.of("key", "value")));
        String errorMessage = underTest.getErrorMessage(new WebApplicationException("error", response));
        assertEquals("{\"key\":\"value\"}", errorMessage);
    }

    @Test
    void testGetErrorMessageWhenWebApplicationExceptionInvalidJsonFallbackToErrorResponse() throws JsonProcessingException {
        Response response = mock(Response.class);
        when(response.hasEntity()).thenReturn(Boolean.TRUE);
        when(response.readEntity(eq(String.class))).thenReturn("invalid");
        String errorMessage = underTest.getErrorMessage(new WebApplicationException("error", response));
        assertEquals("invalid", errorMessage);
    }
}
