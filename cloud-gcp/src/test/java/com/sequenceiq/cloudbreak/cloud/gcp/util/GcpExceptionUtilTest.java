package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;

class GcpExceptionUtilTest {

    @Test
    void testResourceNotFoundExceptionWhenDetailsAreNull() {
        GoogleJsonResponseException exception = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_NOT_FOUND, "Not Found", new HttpHeaders()), null);
        assertFalse(GcpExceptionUtil.resourceNotFoundException(exception));
    }

    @Test
    void testResourceNotFoundExceptionWhenDetailsDoNotContainCode() {
        GoogleJsonError details = new GoogleJsonError();
        GoogleJsonResponseException exception = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_NOT_FOUND, "Not Found", new HttpHeaders()), details);
        assertFalse(GcpExceptionUtil.resourceNotFoundException(exception));
    }

    @Test
    void testResourceNotFoundExceptionWhenCodeIsNotFound() {
        GoogleJsonError details = new GoogleJsonError();
        details.set("code", HttpStatus.SC_NOT_FOUND);
        GoogleJsonResponseException exception = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_NOT_FOUND, "Not Found", new HttpHeaders()), details);
        assertTrue(GcpExceptionUtil.resourceNotFoundException(exception));
    }

    @Test
    void testResourceNotFoundExceptionWhenCodeIsForbidden() {
        GoogleJsonError details = new GoogleJsonError();
        details.set("code", HttpStatus.SC_FORBIDDEN);
        GoogleJsonResponseException exception = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_FORBIDDEN, "Forbidden", new HttpHeaders()), details);
        assertTrue(GcpExceptionUtil.resourceNotFoundException(exception));
    }

    @Test
    void testResourceNotFoundExceptionWhenCodeIsOther() {
        GoogleJsonError details = new GoogleJsonError();
        details.set("code", HttpStatus.SC_INTERNAL_SERVER_ERROR);
        GoogleJsonResponseException exception = new GoogleJsonResponseException(
                new HttpResponseException.Builder(HttpStatus.SC_INTERNAL_SERVER_ERROR, "Internal Server Error", new HttpHeaders()), details);
        assertFalse(GcpExceptionUtil.resourceNotFoundException(exception));
    }
}
