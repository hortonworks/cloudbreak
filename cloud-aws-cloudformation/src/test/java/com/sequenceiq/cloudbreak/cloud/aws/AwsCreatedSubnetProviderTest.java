package com.sequenceiq.cloudbreak.cloud.aws;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.network.CreatedSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetRequest;

public class AwsCreatedSubnetProviderTest {

    private final AwsCreatedSubnetProvider underTest = new AwsCreatedSubnetProvider();

    @Test
    public void testProvideShouldReturnTheSubnetsFromCloudformationOutput() {
        List<SubnetRequest> cidrs = Lists.newArrayList(
                publicSubnetRequest("10.0.0.16/19", 0),
                publicSubnetRequest("10.0.0.16/19", 1),
                publicSubnetRequest("10.0.0.16/19", 2));

        Map<String, String> output = new HashMap<>();
        output.put("id0", "public-subnet-0");
        output.put("id1", "public-subnet-1");
        output.put("id2", "public-subnet-1");

        Set<CreatedSubnet> actual = underTest.provide(output, cidrs, true);

        assertEquals(cidrs.size(), actual.size());
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getSubnetId())));
    }

    @Test
    public void testProvideShouldReturnThePublicSubnetsWhenThePrivateSubnetsAreDisabled() {
        List<SubnetRequest> cidrs = Lists.newArrayList(
                publicSubnetRequest("10.0.0.16/19", 0),
                publicSubnetRequest("10.0.0.16/19", 1),
                publicSubnetRequest("10.0.0.16/19", 2));

        Map<String, String> output = new HashMap<>();
        output.put("id0", "public-subnet-0");
        output.put("id1", "public-subnet-1");
        output.put("id2", "public-subnet-1");

        Set<CreatedSubnet> actual = underTest.provide(output, cidrs, true);

        assertEquals(cidrs.size(), actual.size());
        assertTrue(actual.stream().allMatch(createdSubnet -> output.containsValue(createdSubnet.getSubnetId())));
    }

    @Test
    public void testProvideShouldThrowAnExceptionWhenAValueIsMissingFromTheCloudformationOutput() {
        List<SubnetRequest> cidrs = Lists.newArrayList(
                publicSubnetRequest("10.0.0.16/19", 0),
                publicSubnetRequest("10.0.0.16/19", 1),
                publicSubnetRequest("10.0.0.16/19", 2));
        assertThrows(CloudConnectorException.class, () -> underTest.provide(new HashMap<>(), cidrs, true));
    }

    public SubnetRequest publicSubnetRequest(String cidr, int index) {
        SubnetRequest subnetRequest = new SubnetRequest();
        subnetRequest.setIndex(index);
        subnetRequest.setPublicSubnetCidr(cidr);
        subnetRequest.setSubnetGroup(index % 3);
        subnetRequest.setAvailabilityZone("az");
        return subnetRequest;
    }
}