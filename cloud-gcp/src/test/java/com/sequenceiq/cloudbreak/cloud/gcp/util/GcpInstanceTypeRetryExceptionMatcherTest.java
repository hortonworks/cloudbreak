package com.sequenceiq.cloudbreak.cloud.gcp.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.compute.model.ErrorInfo;
import com.google.api.services.compute.model.Operation;

class GcpInstanceTypeRetryExceptionMatcherTest {

    private static final int TOO_MANY_REQUESTS = 429;

    @Test
    void testIsInstanceTypeNotSupportedWhenUnsupported() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_BAD_REQUEST, "Invalid value for field 'resource.machineType'");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("INVALID_MACHINE_TYPE", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhausted() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "ZONE_RESOURCE_POOL_EXHAUSTED");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhaustedInternal() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "ZONE_RESOURCE_POOL_EXHAUSTED_INTERNAL");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhaustedWith429() {
        GoogleJsonResponseException e = createException(TOO_MANY_REQUESTS, "ZONE_RESOURCE_POOL_EXHAUSTED");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenExhaustedWithDetails() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @ParameterizedTest
    @ValueSource(ints = {HttpStatus.SC_BAD_REQUEST, HttpStatus.SC_FORBIDDEN, HttpStatus.SC_CONFLICT, HttpStatus.SC_INTERNAL_SERVER_ERROR})
    void testIsInstanceTypeNotSupportedWhenExhaustedWithUnexpectedStatusCode(int statusCode) {
        GoogleJsonResponseException e = createException(statusCode, "ZONE_RESOURCE_POOL_EXHAUSTED");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenReasonInErrorInfo() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Service unavailable");
        GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
        errorInfo.setReason("ZONE_RESOURCE_POOL_EXHAUSTED");
        errorInfo.setMessage("Service unavailable");
        e.getDetails().setErrors(List.of(errorInfo));
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenReasonInDetails() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE, "Service unavailable");
        GoogleJsonError.Details details = new GoogleJsonError.Details();
        details.setType("type.googleapis.com/google.rpc.ErrorInfo");
        details.setReason("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS");
        e.getDetails().setDetails(List.of(details));
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenStockoutMessageWithoutCode() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE,
                "The zone 'projects/my-project/zones/us-west1-a' does not have enough resources available to fulfill the request. "
                        + "Try a different zone, or try again later.");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenDetailedStockoutMessageWithoutCode() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_SERVICE_UNAVAILABLE,
                "A n2-standard-8 VM instance is currently unavailable in the us-west1-a zone. "
                        + "Alternatively, you can try your request again with a different VM hardware configuration or at a later time.");
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOtherError() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_BAD_REQUEST, "Some other error");
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("HTTP_400", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenRateLimited() {
        GoogleJsonResponseException e = createException(TOO_MANY_REQUESTS, "Rate Limit Exceeded");
        GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
        errorInfo.setReason("rateLimitExceeded");
        errorInfo.setMessage("Rate Limit Exceeded");
        e.getDetails().setErrors(List.of(errorInfo));
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenQuotaExceeded() {
        GoogleJsonResponseException e = createException(HttpStatus.SC_FORBIDDEN, "Quota 'N2_CPUS' exceeded. Limit: 24.0 in region us-west1.");
        GoogleJsonError.ErrorInfo errorInfo = new GoogleJsonError.ErrorInfo();
        errorInfo.setReason("quotaExceeded");
        e.getDetails().setErrors(List.of(errorInfo));
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenNullDetails() {
        GoogleJsonResponseException e = new GoogleJsonResponseException(new HttpResponseException
                .Builder(HttpStatus.SC_BAD_REQUEST, "Bad Request", new HttpHeaders()), null);
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(e));
        assertEquals("Unknown", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(e));
    }

    @Test
    void testGetGcpErrorCodeForNotificationWhenNullException() {
        assertEquals("Unknown", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification((GoogleJsonResponseException) null));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOperationHasExhaustedCode() {
        Operation operation = createOperation(new Operation.Error.Errors()
                .setCode("ZONE_RESOURCE_POOL_EXHAUSTED")
                .setMessage("The zone 'projects/p/zones/z' does not have enough resources available to fulfill the request."));
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(operation));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(operation));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOperationHasExhaustedWithDetailsCode() {
        Operation operation = createOperation(new Operation.Error.Errors()
                .setCode("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS")
                .setMessage("A n2-standard-8 VM instance is currently unavailable in the us-west1-a zone."));
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(operation));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED_WITH_DETAILS", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(operation));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOperationHasReasonInErrorInfo() {
        Operation operation = createOperation(new Operation.Error.Errors()
                .setCode("RESOURCE_OPERATION_FAILED")
                .setMessage("Operation failed")
                .setErrorDetails(List.of(new Operation.Error.Errors.ErrorDetails()
                        .setErrorInfo(new ErrorInfo().setReason("ZONE_RESOURCE_POOL_EXHAUSTED")))));
        assertTrue(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(operation));
        assertEquals("ZONE_RESOURCE_POOL_EXHAUSTED", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(operation));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOperationHasOtherError() {
        Operation operation = createOperation(new Operation.Error.Errors()
                .setCode("QUOTA_EXCEEDED")
                .setMessage("Quota 'CPUS' exceeded. Limit: 24.0 in region us-west1."));
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(operation));
        assertEquals("QUOTA_EXCEEDED", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(operation));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOperationHasNoError() {
        Operation operation = new Operation().setName("op");
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported(operation));
        assertEquals("Unknown", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification(operation));
    }

    @Test
    void testIsInstanceTypeNotSupportedWhenOperationIsNull() {
        assertFalse(GcpInstanceTypeRetryExceptionMatcher.isInstanceTypeNotSupported((Operation) null));
        assertEquals("Unknown", GcpInstanceTypeRetryExceptionMatcher.getGcpErrorCodeForNotification((Operation) null));
    }

    private GoogleJsonResponseException createException(int statusCode, String message) {
        GoogleJsonError error = new GoogleJsonError();
        error.setMessage(message);
        error.setCode(statusCode);
        return new GoogleJsonResponseException(new HttpResponseException.Builder(statusCode, "Error", new HttpHeaders()), error);
    }

    private Operation createOperation(Operation.Error.Errors errors) {
        return new Operation()
                .setName("op")
                .setError(new Operation.Error().setErrors(List.of(errors)));
    }
}
