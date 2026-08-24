package com.sequenceiq.cloudbreak.cm.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.client.ApiException;

class ClouderaManagerOperationFailedExceptionTest {

    @Test
    void apiExceptionConstructorUsesExtractedResponseBodyMessageAndKeepsCause() {
        String cmMessage = "A previous unfinished upgrade command was found.";
        ApiException apiException = new ApiException("Bad Request", 400, null, "{\"message\":\"" + cmMessage + "\"}");

        ClouderaManagerOperationFailedException exception = new ClouderaManagerOperationFailedException(apiException);

        assertEquals(cmMessage, exception.getMessage());
        assertSame(apiException, exception.getCause());
    }

    @Test
    void apiExceptionConstructorFallsBackToHttpMessageWhenResponseBodyIsMissing() {
        ApiException apiException = new ApiException(400, "HTTP 400 Bad Request");

        ClouderaManagerOperationFailedException exception = new ClouderaManagerOperationFailedException(apiException);

        assertEquals("HTTP 400 Bad Request", exception.getMessage());
        assertSame(apiException, exception.getCause());
    }
}
