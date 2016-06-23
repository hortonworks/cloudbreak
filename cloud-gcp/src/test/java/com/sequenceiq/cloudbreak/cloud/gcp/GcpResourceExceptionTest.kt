package com.sequenceiq.cloudbreak.cloud.gcp

import org.junit.Assert
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.sequenceiq.cloudbreak.common.type.ResourceType

class GcpResourceExceptionTest {

    @Test
    fun shouldKeepTheCauseMessage() {
        //GIVEN
        val causeMessage = "Cause Message"
        val throwable = Exception(causeMessage)
        //WHEN

        val subject = GcpResourceException("New Message", throwable)

        //THEN
        Assert.assertEquals("The exception message is not correct!", "New Message\n [ Cause message: $causeMessage ]\n", subject.message)
    }

    @Test
    @Throws(Exception::class)
    fun shouldMessageBeWellFormattedWhenUsingCustomConstructor() {
        //GIVEN

        //WHEN
        val subject = GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name")

        //THEN
        Assert.assertEquals("The error message has not the right format!", "Error!: [ resourceType: GCP_DISK,  resourceName: disk-name ]", subject.message)
    }

    @Test
    @Throws(Exception::class)
    fun testShouldMessageBeWellFormattedWhenUsingLongConstructor() {
        //GIVEN

        //WHEN
        val subject = GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name", 123L, "create")

        //THEN
        Assert.assertEquals("The error message has not the right format!",
                "Error!: [ resourceType: GCP_DISK,  resourceName: disk-name, stackId: 123, operation: create ]", subject.message)

    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(GcpResourceExceptionTest::class.java)
    }

}