package com.sequenceiq.cloudbreak.cloud.azure.util;

import static jakarta.ws.rs.core.Response.Status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.microsoft.aad.msal4j.MsalServiceException;

class MsalServiceExceptionMapperTest {

    private MsalServiceExceptionMapper underTest = new MsalServiceExceptionMapper();

    @Test
    void testGetResponseStatusWhenErrorCodeIs400() {
        MsalServiceException msalServiceException = mock(MsalServiceException.class);
        when(msalServiceException.statusCode()).thenReturn(400);
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(msalServiceException));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIs401() {
        MsalServiceException msalServiceException = mock(MsalServiceException.class);
        when(msalServiceException.statusCode()).thenReturn(401);
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(msalServiceException));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIs403() {
        MsalServiceException msalServiceException = mock(MsalServiceException.class);
        when(msalServiceException.statusCode()).thenReturn(403);
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(msalServiceException));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIsOtherThanAbove() {
        MsalServiceException msalServiceException = mock(MsalServiceException.class);
        when(msalServiceException.statusCode()).thenReturn(503);
        assertEquals(Status.INTERNAL_SERVER_ERROR, underTest.getResponseStatus(msalServiceException));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIsNull() {
        MsalServiceException msalServiceException = mock(MsalServiceException.class);
        when(msalServiceException.statusCode()).thenReturn(null);
        assertEquals(Status.INTERNAL_SERVER_ERROR, underTest.getResponseStatus(msalServiceException));
    }
}