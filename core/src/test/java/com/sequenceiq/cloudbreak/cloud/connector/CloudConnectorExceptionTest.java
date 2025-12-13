package com.sequenceiq.cloudbreak.cloud.connector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

class CloudConnectorExceptionTest {

    public static final String CAUSE_MESSAGE = "This is the cause message";

    @Test
    void shouldKeepDetailedMessageWhenSingleArgConstructorCalled() {
        // GIVEN
        Throwable throwable = new IllegalArgumentException(CAUSE_MESSAGE);

        // WHEN
        CloudConnectorException cloudConnectorException = new CloudConnectorException(throwable);

        // THEN
        assertEquals(CAUSE_MESSAGE, cloudConnectorException.getCause().getMessage(), "Invalid cause message");
        assertEquals(throwable.getClass(), cloudConnectorException.getCause().getClass(), "Unexpected cause");
    }

}