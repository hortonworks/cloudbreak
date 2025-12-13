package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class AwsDBStackV4ParametersTest {

    private AwsDBStackV4Parameters underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AwsDBStackV4Parameters();
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

}
