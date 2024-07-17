package com.sequenceiq.cloudbreak.cloud.azure.util;

import static jakarta.ws.rs.core.Response.Status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.azure.core.http.HttpResponse;
import com.azure.core.management.exception.ManagementException;

class ManagementExceptionMapperTest {

    private ManagementExceptionMapper underTest = new ManagementExceptionMapper();

    @Test
    void testGetResponseStatusWhenErrorCodeIs400() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(400);
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(new ManagementException("error", httpResponse)));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIs401() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(401);
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(new ManagementException("error", httpResponse)));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIs403() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(403);
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(new ManagementException("error", httpResponse)));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIsOtherThanAbove() {
        HttpResponse httpResponse = mock(HttpResponse.class);
        when(httpResponse.getStatusCode()).thenReturn(503);
        assertEquals(Status.INTERNAL_SERVER_ERROR, underTest.getResponseStatus(new ManagementException("error", httpResponse)));
    }

    @Test
    void testGetResponseStatusWhenResponseIsNull() {
        assertEquals(Status.INTERNAL_SERVER_ERROR, underTest.getResponseStatus(new ManagementException("error", null)));
    }
}