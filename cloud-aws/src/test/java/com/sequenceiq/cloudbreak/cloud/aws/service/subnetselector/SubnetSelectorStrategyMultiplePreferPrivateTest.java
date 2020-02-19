package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.SUBNET_1;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.SUBNET_4;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.ws.rs.BadRequestException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;

@RunWith(MockitoJUnitRunner.class)
public class SubnetSelectorStrategyMultiplePreferPrivateTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SubnetSelectorService subnetSelectorService;

    @InjectMocks
    private SubnetSelectorStrategyMultiplePreferPrivate underTest;

    @Before
    public void setup() {
        when(subnetSelectorService.collectOnePrivateSubnetPerAz(ArgumentMatchers.any(), anyInt())).thenCallRealMethod();
        when(subnetSelectorService.collectOnePublicSubnetPerAz(ArgumentMatchers.any(), anyInt())).thenCallRealMethod();
        when(subnetSelectorService.collectSubnetsOfMissingAz(anyMap(), anyMap(), anyInt())).thenCallRealMethod();
        underTest.minSubnetCountInDifferentAz = 2;
        underTest.maxSubnetCountInDifferentAz = 3;
    }

    @Test
    public void testSelectInternalWhenPrivatesInTwoDifferentAzThenSubnetsOfTwoAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_A)), hasProperty("id", is(SUBNET_1)))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_B))));
    }

    @Test
    public void testWhenMultiplePrivateSubnetThreeDifferentAzThenOneSubnetPerAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_A)), hasProperty("id", is(SUBNET_1)))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_B))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_C)), hasProperty("id", is(SUBNET_4)))));
    }

    @Test
    public void testWhenSixPrivateSubnetsFromThreeDifferentAzThenOneSubnetPerAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_C)
                .withPrivateSubnet(AZ_C)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(3));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_C))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_B))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_A))));
    }

    @Test
    public void testWhenSixPublicSubnetsFromThreeDifferentAzThenSubnetFromTwoAzsSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertEquals(2L, chosenSubnets.stream().map(CloudSubnet::getAvailabilityZone).distinct().count());
    }

    @Test
    public void testWhenTwoDifferrentPrivateSubnetInTwoAzsOnePublicThenPrivateSubnetsSelectedOnly() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_A))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_B))));
    }

    @Test
    public void testWhenThreeDifferrentPublicSubnetInTwoAzThenTwoSubnetsInTwoAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_C)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_A))));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_C))));
    }

    @Test
    public void testWhenTwoPrivateInSameAzOnePublicDifferentAzThenOnePrivateOnePublicInTwoDifferentAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_A)
                .withPublicSubnet(AZ_C)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_A)),
                hasProperty("privateSubnet", is(true)))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_C)),
                hasProperty("privateSubnet", is(false)))));
    }

    @Test
    public void testWhenTwoPrivateInSameAzOnePublicWithNoIpThenThrowsBadRequest() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_A)
                .withPublicSubnetNoPublicIp(AZ_C)
                .build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Acceptable subnets are in 1 different AZs, but subnets in 2 different AZs required.");

        underTest.selectInternal(subnets);
    }

    @Test
    public void testProperties() {
        assertEquals(2, underTest.getMinimumNumberOfSubnets());
        assertEquals(SubnetSelectorStrategyType.MULTIPLE_PREFER_PRIVATE, underTest.getType());
    }
}
