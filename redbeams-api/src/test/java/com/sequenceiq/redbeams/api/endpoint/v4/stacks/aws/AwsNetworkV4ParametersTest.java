package com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

public class AwsNetworkV4ParametersTest {

    private AwsNetworkV4Parameters underTest;

    @Before
    public void setUp() {
        underTest = new AwsNetworkV4Parameters();
    }

    @Test
    public void testGettersAndSetters() {
        underTest.setSubnetId("subnet-1234");
        assertEquals("subnet-1234", underTest.getSubnetId());
    }

    @Test
    public void testAsMap() {
        underTest.setSubnetId("subnet-1234");

        Map<String, Object> map = underTest.asMap();

        assertEquals("subnet-1234", map.get("subnetId"));
    }

    @Test
    public void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

    @Test
    public void testParse() {
        Map<String, Object> parameters = Map.of("subnetId", "subnet-1234");

        underTest.parse(parameters);

        assertEquals("subnet-1234", underTest.getSubnetId());
    }

}
