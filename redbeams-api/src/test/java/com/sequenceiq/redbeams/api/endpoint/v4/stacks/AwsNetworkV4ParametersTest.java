package com.sequenceiq.redbeams.api.endpoint.v4.stacks;

import static org.junit.Assert.assertEquals;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class AwsNetworkV4ParametersTest {

    private AwsNetworkV4Parameters underTest;

    @Before
    public void setUp() {
        underTest = new AwsNetworkV4Parameters();
    }

    @Test
    public void testGettersAndSetters() {
        underTest.setVpcId("vpc-1234");
        assertEquals("vpc-1234", underTest.getVpcId());

        underTest.setInternetGatewayId("igw-1234");
        assertEquals("igw-1234", underTest.getInternetGatewayId());

        underTest.setSubnetId("subnet-1234");
        assertEquals("subnet-1234", underTest.getSubnetId());
    }

    @Test
    public void testAsMap() {
        underTest.setVpcId("vpc-1234");
        underTest.setInternetGatewayId("igw-1234");
        underTest.setSubnetId("subnet-1234");

        Map<String, Object> map = underTest.asMap();

        assertEquals("vpc-1234", map.get("vpcId"));
        assertEquals("igw-1234", map.get("internetGatewayId"));
        assertEquals("subnet-1234", map.get("subnetId"));
    }

    @Test
    public void testGetCloudPlatform() {
        assertEquals(CloudPlatform.AWS, underTest.getCloudPlatform());
    }

    @Test
    public void testParse() {
        Map<String, Object> parameters = Map.of("vpcId", "vpc-1234", "internetGatewayId", "igw-1234", "subnetId", "subnet-1234");

        underTest.parse(parameters);

        assertEquals("vpc-1234", underTest.getVpcId());
        assertEquals("igw-1234", underTest.getInternetGatewayId());
        assertEquals("subnet-1234", underTest.getSubnetId());
    }

}
