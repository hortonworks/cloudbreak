package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_D;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.SUBNET_1;
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
public class SubnetSelectorStrategyMultiplePreferPublicTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SubnetSelectorService subnetSelectorService;

    @InjectMocks
    private SubnetSelectorStrategyMultiplePreferPublic underTest;

    private final SubnetHelper subnetHelper = new SubnetHelper();

    @Before
    public void setup() {
        when(subnetSelectorService.collectOnePrivateSubnetPerAz(ArgumentMatchers.any(), anyInt())).thenCallRealMethod();
        when(subnetSelectorService.collectOnePublicSubnetPerAz(ArgumentMatchers.any(), anyInt())).thenCallRealMethod();
        when(subnetSelectorService.collectSubnetsOfMissingAz(anyMap(), anyMap(), anyInt())).thenCallRealMethod();
        underTest.minSubnetCountInDifferentAz = 2;
        underTest.maxSubnetCountInDifferentAz = 3;
    }

    @Test
    public void testWhenMultiplePublicSubnetTwoDifferentAzThenOneSubnetPerAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertEquals(2, subnetHelper.countDifferentAZs(chosenSubnets));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_A)), hasProperty("id", is(SUBNET_1)))));
    }

    @Test
    public void testWhenMultiplePublicSubnetThreeDifferentAzThenAllThreeSubnetsSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(3));
        assertEquals(3, subnetHelper.countDifferentAZs(chosenSubnets));
    }

    @Test
    public void testWhenMultiplePublicSubnetFourDifferentAzThenSubnetsFromThreeAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(3));
        assertEquals(3, subnetHelper.countDifferentAZs(chosenSubnets));
    }

    @Test
    public void testWhenMultiplePublicSubnetDifferentAzAndPrivateThenPublicSubnetsOfTwoDifferentAzSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_A))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_B)), hasProperty("privateSubnet", is(false)))));
    }

    @Test
    public void testWhenMultiplePublicNoIpSubnetDifferentAzAndPrivateThenPublicSubnetWithNoIpNotSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnetNoPublicIp(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertThat(chosenSubnets, hasItem(hasProperty("availabilityZone", is(AZ_A))));
        assertThat(chosenSubnets, hasItem(allOf(hasProperty("availabilityZone", is(AZ_B)), hasProperty("privateSubnet", is(true)))));
    }

    @Test
    public void testWhenOnePublicOnePrivateThenBothSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertEquals(2, subnetHelper.countDifferentAZs(chosenSubnets));
    }

    @Test
    public void testWhenTwoPrivateSubnetsDifferentAzThenBothSelected() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(2));
        assertEquals(2, subnetHelper.countDifferentAZs(chosenSubnets));
    }

    @Test
    public void testWhenTwoPublicSameAzThenThrowsBadRequest() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_A)
                .build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Acceptable subnets are in 1 different AZs, but subnets in 2 different AZs required.");

        underTest.selectInternal(subnets);
    }

    @Test
    public void testWhenOnePublicOnePrivateSameAzThenThrowsBadRequest() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPrivateSubnet(AZ_A)
                .build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Acceptable subnets are in 1 different AZs, but subnets in 2 different AZs required.");

        underTest.selectInternal(subnets);
    }

    @Test
    public void testWhenTwoPublicSubnetsWithNoPublicIpThenThrowsBadRequest() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnetNoPublicIp(AZ_B)
                .build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Acceptable subnets are in 1 different AZs, but subnets in 2 different AZs required.");

        underTest.selectInternal(subnets);
    }

    @Test
    public void testWhenPublicSubnetWithNoPublicIpThenThrowsBadRequest() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp(AZ_A)
                .build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("Acceptable subnets are in 0 different AZs, but subnets in 2 different AZs required.");

        underTest.selectInternal(subnets);
    }

    @Test
    public void testProperties() {
        assertEquals(2, underTest.getMinimumNumberOfSubnets());
        assertEquals(SubnetSelectorStrategyType.MULTIPLE_PREFER_PUBLIC, underTest.getType());
    }
}
