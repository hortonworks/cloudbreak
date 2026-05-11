package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;

class GcpInstanceTypeRetryExceptionMatcherTest {

    @Test
    void testIsInstanceTypeNotSupportedWhenUnsupported() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_BAD_REQUEST, "Invalid value for field 'resource.machineType'");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhausted() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "ZONE_RESOURCE_POOL_EXHAUSTED");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhaustedInternal() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "ZONE_RESOURCE_POOL_EXHAUSTED_INTERNAL");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhaustedWith429() {
        GoogleJsonResponseException e = createException(429, "ZONE_RESOURCE_POOL_EXHAUSTED");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOtherError() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_BAD_REQUEST, "Some other error");
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenNullDetails() {
        GoogleJsonResponseException e = new GoogleJsonResponseException(new HttpResponseException
                .Builder(HttpStatus.SC_BAD_REQUEST, "Bad Request", new HttpHeaders()), null);
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    private GoogleJsonResponseException createException(int statusCode, String message) {
        GoogleJsonError error = new GoogleJsonError();
        error.setMessage(message);
        error.setCode(statusCode);
        return new GoogleJsonResponseException(new HttpResponseException.Builder(statusCode, "Error", new HttpHeaders()), error);
    }
}
