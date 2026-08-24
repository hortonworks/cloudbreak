package com.sequenceiq.cloudbreak.cm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.cloudera.api.swagger.client.ApiException;

class ClouderaManagerApiExceptionUtilTest {

    @Test
    void extractMessageReturnsJsonMessageFieldWhenResponseBodyContainsTextualMessage() {
        String cmMessage = "A previous unfinished upgrade command was found. To continue upgrading: perform a 'Retry' on the original command.";
        ApiException apiException = new ApiException("Bad Request", 400, null, "{\"message\":\"" + cmMessage + "\"}");

        assertEquals(cmMessage, ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }

    @Test
    void extractMessageReturnsResponseBodyWhenJsonHasNoMessageField() {
        String responseBody = "{\"cause\":\"something else\"}";
        ApiException apiException = new ApiException("Bad Request", 400, null, responseBody);

        assertEquals(responseBody, ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }

    @Test
    void extractMessageReturnsResponseBodyWhenMessageFieldIsBlank() {
        String responseBody = "{\"message\":\"\"}";
        ApiException apiException = new ApiException("Bad Request", 400, null, responseBody);

        assertEquals(responseBody, ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }

    @Test
    void extractMessageReturnsResponseBodyWhenMessageFieldIsNotTextual() {
        String responseBody = "{\"message\":42}";
        ApiException apiException = new ApiException("Bad Request", 400, null, responseBody);

        assertEquals(responseBody, ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }

    @Test
    void extractMessageReturnsResponseBodyWhenItIsNotValidJson() {
        String responseBody = "Bad Request";
        ApiException apiException = new ApiException("Bad Request", 400, null, responseBody);

        assertEquals(responseBody, ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }

    @Test
    void extractMessageReturnsHttpMessageWhenResponseBodyIsNull() {
        ApiException apiException = new ApiException(400, "HTTP 400 Bad Request");

        assertEquals("HTTP 400 Bad Request", ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }

    @Test
    void extractMessageReturnsHttpMessageWhenResponseBodyIsEmpty() {
        ApiException apiException = new ApiException("HTTP 400 Bad Request", 400, null, "");

        assertEquals("HTTP 400 Bad Request", ClouderaManagerApiExceptionUtil.extractMessage(apiException));
    }
}
