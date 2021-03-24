package com.sequenceiq.freeipa.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RetryableFreeIpaClientExceptionTest {

    @Test
    public void testToStringWhenExceptionForRestApiPresent() {
        RetryableFreeIpaClientException underTest =
                new RetryableFreeIpaClientException("testMsg",
                        new RetryableFreeIpaClientException("testMsg1", new Exception("testMsg2")),
                        new Exception("testMsg3"));
        String result = underTest.toString();
        assertEquals("com.sequenceiq.freeipa.client.RetryableFreeIpaClientException: testMsg\n"
                + "ExceptionForRestApi: java.lang.Exception: testMsg3", result);
    }

    @Test
    public void testToStringWhenExceptionForRestApiMissing() {
        RetryableFreeIpaClientException underTest =
                new RetryableFreeIpaClientException("testMsg",
                        new RetryableFreeIpaClientException("testMsg1", new Exception("testMsg2")));
        String result = underTest.toString();
        assertEquals("com.sequenceiq.freeipa.client.RetryableFreeIpaClientException: testMsg\n"
                + "ExceptionForRestApi: not set", result);
    }
}