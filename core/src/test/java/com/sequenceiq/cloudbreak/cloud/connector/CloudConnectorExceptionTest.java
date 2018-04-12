package com.sequenceiq.cloudbreak.cloud.connector;

import org.junit.Assert;
import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class CloudConnectorExceptionTest {

    public static final String CAUSE_MESSAGE = "This is the cause message";

    @Test
    public void shouldKeepDetailedMessageWhenSingleArgConstructorCalled() {
        // GIVEN
        Throwable throwable = new IllegalArgumentException(CAUSE_MESSAGE);

        // WHEN
        CloudConnectorException cloudConnectorException = new CloudConnectorException(throwable);

        // THEN
        Assert.assertEquals("Invalid cause message", CAUSE_MESSAGE, cloudConnectorException.getCause().getMessage());
        Assert.assertEquals("Unexpected cause", throwable.getClass(), cloudConnectorException.getCause().getClass());
    }

}