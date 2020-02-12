package com.sequenceiq.redbeams.service.network;


import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@RunWith(MockitoJUnitRunner.class)
public class AwsSubnetChooserTest {

    private static final String AVAILABILITY_ZONE_A = "AZ-a";

    private static final String AVAILABILITY_ZONE_B = "AZ-b";

    private static final String AVAILABILITY_ZONE_C = "AZ-c";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final String SUBNET_3 = "subnet-3";

    private static final String SUBNET_4 = "subnet-4";

    private static final Map<String, String> MULTI_AZ_FALSE = Map.of("multiAZ", "false");

    @Mock
    private AwsSubnetValidator awsSubnetValidator;

    @InjectMocks
    private AwsSubnetChooser underTest;

    @Test
    public void testMultiplePrivateSubnetTwoDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_B, "", true, false, false)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)), hasProperty("id", is(SUBNET_1)))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B))));
        verify(awsSubnetValidator).validate(subnets, 2);
    }

    @Test
    public void testMultiplePrivateSubnetThreeDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_4, "", AVAILABILITY_ZONE_C, "", true, false, false)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)), hasProperty("id", is(SUBNET_1)))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_C)), hasProperty("id", is(SUBNET_4)))));
        verify(awsSubnetValidator).validate(subnets, 2);
    }

    @Test
    public void testWithSixPrivateSubnetsFromSomeDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet("subnet1", "", "us-west-2c", "", true, false, false),
                new CloudSubnet("subnet2", "", "us-west-2c", "", true, false, false),
                new CloudSubnet("subnet3", "", "us-west-2b", "", true, false, false),
                new CloudSubnet("subnet4", "", "us-west-2a", "", true, false, false),
                new CloudSubnet("subnet5", "", "us-west-2a", "", true, false, false),
                new CloudSubnet("subnet6", "", "us-west-2b", "", true, false, false)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is("us-west-2c"))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is("us-west-2b"))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is("us-west-2a"))));
        verify(awsSubnetValidator).validate(subnets, 2);
    }

    @Test
    public void testWithSixPublicSubnetsFromSomeDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet("subnet1", "", "us-west-2c", "", false, true, true),
                new CloudSubnet("subnet2", "", "us-west-2c", "", false, true, true),
                new CloudSubnet("subnet3", "", "us-west-2b", "", false, true, true),
                new CloudSubnet("subnet4", "", "us-west-2a", "", false, true, true),
                new CloudSubnet("subnet5", "", "us-west-2a", "", false, true, true),
                new CloudSubnet("subnet6", "", "us-west-2b", "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(2));
        verify(awsSubnetValidator).validate(subnets, 2);
        assertEquals(2L, chosenSubnets.stream().map(CloudSubnet::getAvailabilityZone).distinct().count());
    }

    @Test
    public void testTwoDifferrentPrivAzOnePublic() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B))));
        verify(awsSubnetValidator).validate(subnets, 2);
    }

    @Test
    public void testTwoDifferrentPubAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", false, true, true),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_A, "", false, true, true),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_C))));
        verify(awsSubnetValidator).validate(subnets, 2);
    }

    @Test
    public void testSamePrivAzOnePublic() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, null);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_C)),
                hasProperty("privateSubnet", is(false)))));
        verify(awsSubnetValidator).validate(subnets, 2);
    }

    @Test
    public void testNoMultiAzWithTwoPrivateSameAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(true)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithTwoPrivateDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B)),
                hasProperty("privateSubnet", is(true)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithThreePrivateDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", true, false, false)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_C)),
                hasProperty("privateSubnet", is(true)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithFourPrivateDifferentAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, false, false),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", true, false, false),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_C, "", true, false, false),
                new CloudSubnet(SUBNET_4, "", AVAILABILITY_ZONE_C, "", true, false, false)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_B)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_C)),
                hasProperty("privateSubnet", is(true)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithOnePublic() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(false)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithTwoPublicDiffAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", false, true, true),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(hasProperty("privateSubnet", is(false))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithTwoPublicSameAz() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", false, true, true),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_A, "", false, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(false)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }

    @Test
    public void testNoMultiAzWithOnePrivate() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, "", true, true, true)
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, MULTI_AZ_FALSE);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AVAILABILITY_ZONE_A)),
                hasProperty("privateSubnet", is(true)))));
        verify(awsSubnetValidator).validate(subnets, 1);
    }
}