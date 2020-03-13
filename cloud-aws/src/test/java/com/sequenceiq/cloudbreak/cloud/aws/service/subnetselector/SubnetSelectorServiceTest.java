package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;


import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_D;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;
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

        List<CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectPrivateSubnets(cloudSubnets);

        assertTrue(privateSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenOnePrivateSubnetInThreeDifferentAzsThenReturnsAllSubnets() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .build();

        List<CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectPrivateSubnets(cloudSubnets);

        assertThat(privateSubnetsPerAz, hasSize(3));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenSubnetsInMoreThanThreeAzsThenReturnsSubnetsFromThreeAzsOnly() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .withPrivateSubnet(AZ_D)
                .build();

        List<CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectPrivateSubnets(cloudSubnets);

        assertThat(privateSubnetsPerAz, hasSize(4));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenTwoInOneAzThenReturnsOnlyOneSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_A)
                .build();

        List<CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectPrivateSubnets(cloudSubnets);

        assertThat(privateSubnetsPerAz, hasSize(2));
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenOnePublicOnePrivateThenReturnsOnlyThePrivateSubnet() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .build();

        List<CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectPrivateSubnets(cloudSubnets);

        assertThat(privateSubnetsPerAz, hasSize(1));
        assertTrue(getSubnetByAz(privateSubnetsPerAz, AZ_A).isPresent());
    }

    @Test
    public void testCollectOnePrivateSubnetPerAzWhenPublicOnlyThenReturnsEmptyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .build();

        List<CloudSubnet> privateSubnetsPerAz = subnetSelectorService.collectPrivateSubnets(cloudSubnets);

        assertTrue(privateSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenEmptyThenReturnsEmpty() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertTrue(publicSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenThreePublicInDifferentAzsThenReturnsAllSubnets() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertThat(publicSubnetsPerAz, hasSize(3));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenSubnetsInMoreThanThreeAzsThenReturnsSubnetsOfThreeAzsOnly() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertThat(publicSubnetsPerAz, hasSize(4));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenTwoPublicInOneAzThenReturnsOneSubnetOnly() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_A)
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertThat(publicSubnetsPerAz, hasSize(2));
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenOnePublicOnePrivateThenReturnsOnePublic() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertThat(publicSubnetsPerAz, hasSize(1));
        assertTrue(getSubnetByAz(publicSubnetsPerAz, AZ_A).isPresent());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenPrivateOnlyThenReturnsEmptyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertTrue(publicSubnetsPerAz.isEmpty());
    }

    @Test
    public void testCollectOnePublicSubnetPerAzWhenPublicWithNoPublicIpThenReturnsEmptyMap() {
        List<CloudSubnet> cloudSubnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp(AZ_A)
                .build();

        List<CloudSubnet> publicSubnetsPerAz = subnetSelectorService.collectPublicSubnets(cloudSubnets);

        assertTrue(publicSubnetsPerAz.isEmpty());
    }

    private Optional<CloudSubnet> getSubnetByAz(List<CloudSubnet> cloudSubnets, String az) {
        return cloudSubnets.stream().filter(e -> e.getAvailabilityZone().equals(az)).findFirst();
    }
}
