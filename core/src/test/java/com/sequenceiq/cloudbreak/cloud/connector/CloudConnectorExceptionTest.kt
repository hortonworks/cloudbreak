package com.sequenceiq.cloudbreak.cloud.connector

import org.junit.Assert
import org.junit.Test

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException

class CloudConnectorExceptionTest {

    @Test
    fun shouldKeepDetailedMessageWhenSingleArgConstructorCalled() {
        // GIVEN
        val throwable = IllegalArgumentException(CAUSE_MESSAGE)

        // WHEN
        val cloudConnectorException = CloudConnectorException(throwable)

        // THEN
        Assert.assertEquals("Invalid cause message", CAUSE_MESSAGE, cloudConnectorException.cause.message)
        Assert.assertEquals("Unexpected cause", throwable.javaClass, cloudConnectorException.cause.javaClass)
    }

    companion object {

        val CAUSE_MESSAGE = "This is the cause message"
    }

}