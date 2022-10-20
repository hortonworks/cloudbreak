package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class AwsDBStackV4ParametersTest {

    private AwsDBStackV4Parameters underTest;

    @Before
    public void setUp() {
        underTest = new AwsDBStackV4Parameters();
    }

    @Test
    public void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

}
