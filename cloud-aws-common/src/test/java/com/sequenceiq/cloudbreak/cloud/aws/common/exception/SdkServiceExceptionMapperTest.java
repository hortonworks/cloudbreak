package com.sequenceiq.cloudbreak.cloud.aws.common.exception;

import static jakarta.ws.rs.core.Response.Status;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import software.amazon.awssdk.core.exception.SdkServiceException;

class SdkServiceExceptionMapperTest {

    private SdkServiceExceptionMapper underTest = new SdkServiceExceptionMapper();

    @Test
    void testGetResponseStatusWhenErrorCodeIs400() {
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(SdkServiceException.builder().statusCode(400).build()));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIs401() {
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(SdkServiceException.builder().statusCode(401).build()));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIs403() {
        assertEquals(Status.BAD_REQUEST, underTest.getResponseStatus(SdkServiceException.builder().statusCode(403).build()));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIsOtherThanAbove() {
        assertEquals(Status.INTERNAL_SERVER_ERROR, underTest.getResponseStatus(SdkServiceException.builder().statusCode(503).build()));
    }

    @Test
    void testGetResponseStatusWhenErrorCodeIsNotSpecified() {
        assertEquals(Status.INTERNAL_SERVER_ERROR, underTest.getResponseStatus(SdkServiceException.builder().build()));
    }
}