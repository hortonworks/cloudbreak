package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.exception.BadRequestException;

public class SubnetChooserServiceTest {

    private static final String AVAILABILITY_ZONE_A = "AZ-a";

    private static final String AVAILABILITY_ZONE_B = "AZ-b";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final String SUBNET_3 = "subnet-3";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final SubnetChooserService underTest = new SubnetChooserService();

    @Test
    public void testChooseSubnetsAwsSuccess() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, ""),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_B, "")
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, CloudPlatform.AWS);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B))));
    }

    @Test
    public void testChooseSubnetsAwsOnlyOneSubnet() {
        List<CloudSubnet> subnets = List.of(new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""));
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Insufficient number of subnets");

        underTest.chooseSubnets(subnets, CloudPlatform.AWS);
    }

    @Test
    public void testChooseSubnetsAwsNoSubnets() {
        List<CloudSubnet> subnets = List.of();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Insufficient number of subnets");

        underTest.chooseSubnets(subnets, CloudPlatform.AWS);
    }

    @Test
    public void testChooseSubnetsAwsWithTwoSubnetsFromSameAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_A, "")
        );
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("All subnets are in the same availability zone");

        underTest.chooseSubnets(subnets, CloudPlatform.AWS);
    }

    @Test
    public void testChooseSubnetsAwsWithSixSubnetsFromSomeDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet("subnet1", "", "us-west-2c", ""),
                new CloudSubnet("subnet2", "", "us-west-2c", ""),
                new CloudSubnet("subnet3", "", "us-west-2b", ""),
                new CloudSubnet("subnet4", "", "us-west-2a", ""),
                new CloudSubnet("subnet5", "", "us-west-2a", ""),
                new CloudSubnet("subnet6", "", "us-west-2b", "")
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, CloudPlatform.AWS);

        assertThat(chosenSubnets, hasSize(6));
    }

    // ---

    @Test
    public void testChooseSubnetsAzure() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, ""),
                new CloudSubnet(SUBNET_2, "")
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, CloudPlatform.AZURE);

        assertEquals(subnets, chosenSubnets);
    }

}
