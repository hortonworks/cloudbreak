package com.sequenceiq.redbeams.api.endpoint.v4.stacks.azure;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class AzureDBStackV4ParametersTest {

    private AzureDBStackV4Parameters underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AzureDBStackV4Parameters();
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AZURE, underTest.getCloudPlatform());
    }

}
