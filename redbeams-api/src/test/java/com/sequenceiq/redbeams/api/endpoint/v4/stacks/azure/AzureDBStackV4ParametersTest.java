package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class AzureDBStackV4ParametersTest {

    private AzureDBStackV4Parameters underTest;

    @Before
    public void setUp() {
        underTest = new AzureDBStackV4Parameters();
    }

    @Test
    public void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, underTest.getCloudPlatform());
    }

}
