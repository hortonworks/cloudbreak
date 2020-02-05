package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;

public class AwsCreatedSubnetProviderTest {

    private static final int NUMBER_OF_SUBNETS = 6;

    private static final int NUMBER_OF_PUBLIC_SUBNETS = 3;

    private AwsCreatedSubnetProvider underTest = new AwsCreatedSubnetProvider();

    @Test
    public void testProvideShouldReturnTheSubnetsFromCloudformationOutput() {
        Map<String, String> output = new HashMap<>();
        output.put("PublicSubnetId0", "public-subnet-0");
        output.put("PrivateSubnetId0", "private-subnet-0");
        output.put("PublicSubnetCidr0", "10.0.0.16/19");
        output.put("PrivateSubnetCidr0", "10.0.0.32/19");
        output.put("Az0", "eu-west-a");

        output.put("PublicSubnetId1", "public-subnet-1");
        output.put("PrivateSubnetId1", "private-subnet-1");
        output.put("PublicSubnetCidr1", "10.0.0.16/19");
        output.put("PrivateSubnetCidr1", "10.0.0.32/19");
        output.put("Az1", "eu-west-a");

        output.put("PublicSubnetId2", "public-subnet-1");
        output.put("PrivateSubnetId2", "private-subnet-1");
        output.put("PublicSubnetCidr2", "10.0.0.16/19");
        output.put("PrivateSubnetCidr2", "10.0.0.32/19");
        output.put("Az2", "eu-west-a");

        Set<CreatedSubnet> actual = underTest.provide(output, NUMBER_OF_SUBNETS, true);

        assertEquals(NUMBER_OF_SUBNETS, actual.size());
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getSubnetId())));
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getCidr())));
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getAvailabilityZone())));
    }

    @Test
    public void testProvideShouldReturnThePublicSubnetsWhenThePrivateSubnetsAreDisabled() {
        Map<String, String> output = new HashMap<>();
        output.put("PublicSubnetId0", "public-subnet-0");
        output.put("PublicSubnetCidr0", "10.0.0.16/19");
        output.put("Az0", "eu-west-a");

        output.put("PublicSubnetId1", "public-subnet-1");
        output.put("PublicSubnetCidr1", "10.0.0.16/19");
        output.put("Az1", "eu-west-a");

        output.put("PublicSubnetId2", "public-subnet-1");
        output.put("PublicSubnetCidr2", "10.0.0.16/19");
        output.put("Az2", "eu-west-a");

        Set<CreatedSubnet> actual = underTest.provide(output, NUMBER_OF_SUBNETS, false);

        assertEquals(NUMBER_OF_PUBLIC_SUBNETS, actual.size());
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getSubnetId())));
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getCidr())));
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getAvailabilityZone())));
    }

    @Test(expected = CloudConnectorException.class)
    public void testProvideShouldThrowAnExceptionWhenAValueIsMissingFromTheCloudformationOutput() {
        underTest.provide(new HashMap<>(), NUMBER_OF_SUBNETS, true);
    }
}