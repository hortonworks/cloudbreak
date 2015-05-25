package com.sequenceiq.cloudbreak.service.stack.connector.gcp;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.domain.ResourceType;

public class GcpResourceExceptionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceExceptionTest.class);

    @Test
    public void shouldKeepTheCauseMessage() {
        //GIVEN
        String causeMessage = "Cause Message";
        Throwable throwable = new Exception(causeMessage);
        //WHEN

        GcpResourceException subject = new GcpResourceException("New Message", throwable);

        //THEN
        Assert.assertEquals("The exception message is not correct!", "New Message\n [ Cause message: " + causeMessage + " ]\n", subject.getMessage());
    }

    @Test
    public void shouldMessageBeWellFormattedWhenUsingCustomConstructor() throws Exception {
        //GIVEN

        //WHEN
        GcpResourceException subject = new GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name");

        //THEN
        Assert.assertEquals("The error message has not the right format!", "Error!: [ resourceType: GCP_DISK,  resourceName: disk-name ]", subject.getMessage());
    }

    @Test
    public void testShouldMessageBeWellFormattedWhenUsingLongConstructor() throws Exception {
        //GIVEN

        //WHEN
        GcpResourceException subject = new GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name", 123L, "create");

        //THEN
        Assert.assertEquals("The error message has not the right format!",
                "Error!: [ resourceType: GCP_DISK,  resourceName: disk-name, stackId: 123, operation: create ]", subject.getMessage());

    }

}