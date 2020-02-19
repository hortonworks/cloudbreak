package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;


import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_D;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@RunWith(MockitoJUnitRunner.class)
public class SubnetSelectorServiceTest {

    private final SubnetSelectorService subnetSelectorService = new SubnetSelectorService();

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenEmptyThenReturnsEmtpyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .build();

        Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(cloudSubnets, 3);

        assertTrue(privateSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenOnePrivateSubnetInThreeDifferentAzsThenReturnsAllSubnets() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .build();

        Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(cloudSubnets, 3);

        assertThat(privateSubnetsPerAz.values(), hasSize(3));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenSubnetsInMoreThanThreeAzsThenReturnsSubnetsFromThreeAzsOnly() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .withPrivateSubnet(AZ_D)
                .build();

        Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(cloudSubnets, 3);

        assertThat(privateSubnetsPerAz.values(), hasSize(3));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenTwoInOneAzThenReturnsOnlyOneSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_A)
                .build();

        Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(cloudSubnets, 3);

        assertThat(privateSubnetsPerAz.values(), hasSize(1));
        assertTrue(privateSubnetsPerAz.containsKey(AZ_A));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenOnePublicOnePrivateThenReturnsOnlyThePrivateSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .build();

        Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(cloudSubnets, 3);

        assertThat(privateSubnetsPerAz.values(), hasSize(1));
        assertTrue(privateSubnetsPerAz.containsKey(AZ_A));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenPublicOnlyThenReturnsEmptyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .build();

        Map<String, CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectOnePrivateSubnetPerAz(cloudSubnets, 3);

        assertTrue(privateSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenEmptyThenReturnsEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertTrue(publicSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenThreePublicInDifferentAzsThenReturnsAllSubnets() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertThat(publicSubnetsPerAz.values(), hasSize(3));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenSubnetsInMoreThanThreeAzsThenReturnsSubnetsOfThreeAzsOnly() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertThat(publicSubnetsPerAz.values(), hasSize(3));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenTwoPublicInOneAzThenReturnsOneSubnetOnly() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_A)
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertThat(publicSubnetsPerAz.values(), hasSize(1));
        assertTrue(publicSubnetsPerAz.containsKey(AZ_A));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenOnePublicOnePrivateThenReturnsOnePublic() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertThat(publicSubnetsPerAz.values(), hasSize(1));
        assertTrue(publicSubnetsPerAz.containsKey(AZ_A));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenPrivateOnlyThenReturnsEmptyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertTrue(publicSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenPublicWithNoPublicIpThenReturnsEmptyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp(AZ_A)
                .build();

        Map<String, CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectOnePublicSubnetPerAz(cloudSubnets, 3);

        assertTrue(publicSubnetsPerAz.isEmpty());
    }

    @Test
    public void testGetOnePrivateSubnetWhenEmptyThenReturnsOptionalEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePrivateSubnet(cloudSubnets);

        assertTrue(foundSubnet.isEmpty());
    }

    @Test
    public void testGetOnePrivateSubnetWhenPrivateThenReturnsTheSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePrivateSubnet(cloudSubnets);

        assertTrue(foundSubnet.isPresent());
    }

    @Test
    public void testGetOnePrivateSubnetWhenPublicThenReturnsOptionalEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePrivateSubnet(cloudSubnets);

        assertTrue(foundSubnet.isEmpty());
    }

    @Test
    public void testGetOnePrivateSubnetWhenPublicAndPrivateThenReturnsThePrivateSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet()
                .withPrivateSubnet()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePrivateSubnet(cloudSubnets);

        assertTrue(foundSubnet.isPresent());
        assertTrue(foundSubnet.get().isPrivateSubnet());
    }

    @Test
    public void testGetOnePublicSubnetWhenEmptyThenReturnsOptionalEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePublicSubnet(cloudSubnets);

        assertTrue(foundSubnet.isEmpty());
    }

    @Test
    public void testGetOnePublicSubnetWhenPublicThenReturnsOptionalOfTheSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePublicSubnet(cloudSubnets);

        assertTrue(foundSubnet.isPresent());
    }

    @Test
    public void testGetOnePublicSubnetWhenPrivateThenReturnsOptionalEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePublicSubnet(cloudSubnets);

        assertTrue(foundSubnet.isEmpty());
    }

    @Test
    public void testGetOnePublicSubnetWhenPublicWithNoPublicIpThenReturnsOptionalEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePublicSubnet(cloudSubnets);

        assertTrue(foundSubnet.isEmpty());
    }

    @Test
    public void testGetOnePublicSubnetWhenPublicAndPrivateThenReturnsOptionalOfPublicSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet()
                .withPrivateSubnet()
                .build();

        Optional<CloudSubnet> foundSubnet = subnetSelectorService.getOnePublicSubnet(cloudSubnets);

        assertTrue(foundSubnet.isPresent());
        assertFalse(foundSubnet.get().isPrivateSubnet());
    }

    @Test
    public void testCollectSubnetsOfMissingAzWhenTwoDifferentAzsThenReturnsBothAzs() {
        Map<String, CloudSubnet> subnetPerAz1 = getSubnetsPerAZ(List.of(AZ_A));
        Map<String, CloudSubnet> subnetPerAz2 = getSubnetsPerAZ(List.of(AZ_B));

        Map<String, CloudSubnet> subnets = subnetSelectorService.collectSubnetsOfMissingAz(subnetPerAz1, subnetPerAz2, 2);

        assertEquals(2, subnets.size());
    }

    @Test
    public void testCollectSubnetsOfMissingAzWhenSameAzIsAddedThenKeepsSubnetAlreadyPresent() {
        Map<String, CloudSubnet> subnetPerAz1 = getSubnetsPerAZ(List.of(AZ_A));
        Map<String, CloudSubnet> subnetPerAz2 = getSubnetsPerAZ(List.of(AZ_A, AZ_A));

        Map<String, CloudSubnet> subnets = subnetSelectorService.collectSubnetsOfMissingAz(subnetPerAz1, subnetPerAz2, 2);

        assertEquals(1, subnets.size());
        assertEquals("subnet-1", subnets.get(AZ_A).getId());
    }

    @Test
    public void testCollectSubnetsOfMissingAzWhenMoreSubnetsThanMaxThenNoSubnetsWithNewAzAdded() {
        Map<String, CloudSubnet> subnetPerAz1 = getSubnetsPerAZ(List.of(AZ_A, AZ_B));
        Map<String, CloudSubnet> subnetPerAz2 = getSubnetsPerAZ(List.of(AZ_C));

        Map<String, CloudSubnet> subnets = subnetSelectorService.collectSubnetsOfMissingAz(subnetPerAz1, subnetPerAz2, 2);

        assertEquals(2, subnets.size());
        assertTrue(subnets.containsKey(AZ_A));
        assertTrue(subnets.containsKey(AZ_B));
    }

    private Map<String, CloudSubnet> getSubnetsPerAZ(List<String> azs) {
        Map<String, CloudSubnet> subnetsPerAz = new HashMap<>();
        int subnetId = 1;
        for (String az : azs) {
            subnetsPerAz.putIfAbsent(az, new CloudSubnet("subnet-" + subnetId, "", az, "", true, false, false));
        }
        return subnetsPerAz;
    }
}
