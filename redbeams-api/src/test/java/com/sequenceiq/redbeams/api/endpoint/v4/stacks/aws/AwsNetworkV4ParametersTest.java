package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

class AwsNetworkV4ParametersTest {

    private AwsNetworkV4Parameters underTest;

    @BeforeEach
    public void setUp() {
        underTest = new AwsNetworkV4Parameters();
    }

    @Test
    void testGettersAndSetters() {
        underTest.setSubnetId("subnet-1234");
        assertEquals("subnet-1234", underTest.getSubnetId());
    }

    @Test
    void testAsMap() {
        underTest.setSubnetId("subnet-1234");

        Map<String, Object> map = underTest.asMap();

        assertEquals("subnet-1234", map.get("subnetId"));
    }

    @Test
    void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

    @Test
    void testParse() {
        Map<String, Object> parameters = Map.of("subnetId", "subnet-1234");

        underTest.parse(parameters);

        assertEquals("subnet-1234", underTest.getSubnetId());
    }

}
