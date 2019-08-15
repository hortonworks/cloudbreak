package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.redbeams.exception.BadRequestException;

public class SubnetChooserServiceTest {

    private static final String AVAILABILITY_ZONE_A = "AZ-a";

    private static final String AVAILABILITY_ZONE_B = "AZ-b";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final SubnetChooserService underTest = new SubnetChooserService();

    @Test
    public void testChooseSubnetsFromDifferentAzs() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "")
        );

        List<CloudSubnet> networks = underTest.chooseSubnetsFromDifferentAzs(subnets);

        assertThat(networks, hasSize(2));
        assertThat(networks, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A))));
        assertThat(networks, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B))));
    }

    @Test
    public void testOneSubnet() {
        List<CloudSubnet> subnets = List.of(new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Insufficient number of subnets");

        underTest.chooseSubnetsFromDifferentAzs(subnets);
    }

    @Test
    public void testNoSubnets() {
        List<CloudSubnet> subnets = List.of();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Insufficient number of subnets");

        underTest.chooseSubnetsFromDifferentAzs(subnets);
    }

    @Test
    public void testTwoSubnetsFromSameAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_A, "")
        );
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("All subnets in the same availability zone");

        underTest.chooseSubnetsFromDifferentAzs(subnets);
    }

    @Test
    public void testSixSubnetsFromSomeDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet("subnet1", "", "us-west-2c", ""),
                new CloudSubnet("subnet2", "", "us-west-2c", ""),
                new CloudSubnet("subnet3", "", "us-west-2b", ""),
                new CloudSubnet("subnet4", "", "us-west-2a", ""),
                new CloudSubnet("subnet5", "", "us-west-2a", ""),
                new CloudSubnet("subnet6", "", "us-west-2b", "")
        );

        List<CloudSubnet> networks = underTest.chooseSubnetsFromDifferentAzs(subnets);

        assertThat(networks, hasSize(2));
        String az1 = networks.get(0).getAvailabilityZone();
        String az2 = networks.get(1).getAvailabilityZone();
        assertNotEquals(az1, az2);
    }
}
