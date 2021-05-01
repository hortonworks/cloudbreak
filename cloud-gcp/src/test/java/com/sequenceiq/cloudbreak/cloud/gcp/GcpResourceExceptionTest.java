package com.sequenceiq.cloudbreak.cloud.gcp;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.common.api.type.ResourceType;

public class GcpResourceExceptionTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GcpResourceExceptionTest.class);

    @Test
    public void shouldKeepTheCauseMessage() {
        String causeMessage = "Cause Message";
        Throwable throwable = new Exception(causeMessage);
        GcpResourceException subject = new GcpResourceException("New Message", throwable);
        Assert.assertEquals("The exception message is not correct!", "New Message\n [ Cause message: " + causeMessage + " ]\n", subject.getMessage());
    }

    @Test
    public void shouldKeepTheCauseMessageWhenCallingThrowableConstructor() {
        String causeMessage = "Cause Message";
        Throwable throwable = new Exception(causeMessage);
        GcpResourceException subject = new GcpResourceException(throwable);
        Assert.assertEquals("java.lang.Exception: Cause Message", subject.getMessage());
    }

    @Test
    public void shouldKeepTheCauseMessageWhenCallingMessageConstructor() {
        String causeMessage = "Cause Message";
        GcpResourceException subject = new GcpResourceException(causeMessage);
        Assert.assertEquals("Cause Message", subject.getMessage());
    }

    @Test
    public void shouldMessageBeWellFormattedWhenUsingCustomConstructor() {
        GcpResourceException subject = new GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name");
        Assert.assertEquals("The error message has not the right format!", "Error!: [ resourceType: GCP_DISK,  resourceName: disk-name ]", subject.getMessage());
    }

    @Test
    public void shouldMessageBeWellFormattedWhenUsingCustomConstructorNumberThree() {
        String causeMessage = "Cause Message";
        Throwable throwable = new Exception(causeMessage);
        GcpResourceException subject = new GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name", throwable);
        Assert.assertEquals("Error!: [ resourceType: GCP_DISK,  resourceName: disk-name ]\n" +
                " [ Cause message: Cause Message ]\n", subject.getMessage());
    }

    @Test
    public void testShouldMessageBeWellFormattedWhenUsingLongConstructor() {
        GcpResourceException subject = new GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name", 123L, "create");
        Assert.assertEquals("The error message has not the right format!",
                "Error!: [ resourceType: GCP_DISK,  resourceName: disk-name, stackId: 123, operation: create ]", subject.getMessage());

    }

    @Test
    public void testShouldMessageBeWellFormattedWhenUsingLongConstructorWithThrowable() {
        String causeMessage = "Cause Message";
        Throwable throwable = new Exception(causeMessage);
        GcpResourceException subject = new GcpResourceException("Error!", ResourceType.GCP_DISK, "disk-name", 123L, "create", throwable);
        Assert.assertEquals("Error!: [ resourceType: GCP_DISK,  resourceName: disk-name, stackId: 123, operation: create ]\n" +
                " [ Cause message: Cause Message ]\n", subject.getMessage());

    }

}