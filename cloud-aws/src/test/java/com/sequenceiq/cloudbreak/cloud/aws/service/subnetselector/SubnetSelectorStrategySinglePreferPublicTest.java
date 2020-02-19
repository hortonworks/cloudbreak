package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
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
public class SubnetSelectorStrategySinglePreferPublicTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SubnetSelectorService subnetSelectorService;

    @InjectMocks
    private SubnetSelectorStrategySinglePreferPublic underTest;

    private final SubnetHelper subnetHelper = new SubnetHelper();

    @Before
    public void setup() {
        when(subnetSelectorService.getOnePrivateSubnet(ArgumentMatchers.any())).thenCallRealMethod();
        when(subnetSelectorService.getOnePublicSubnet(ArgumentMatchers.any())).thenCallRealMethod();
    }

    @Test
    public void testWhenOnePublicSubnetThenPublicReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet()
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertFalse(chosenSubnets.isEmpty());
    }

    @Test
    public void testWhenOnePrivateSubnetThenPrivateReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet()
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertFalse(chosenSubnets.isEmpty());
    }

    @Test
    public void testWhenOnePublicAndOnePrivateSubnetThenPublicReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet()
                .withPrivateSubnet()
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(hasProperty("privateSubnet", is(false))));
    }

    @Test
    public void testWhenOnePublicNoPublicIpAndOnePrivateSubnetThenPrivateReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp()
                .withPrivateSubnet()
                .build();

        List<CloudSubnet> chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets, hasSize(1));
        assertThat(chosenSubnets, hasItem(hasProperty("privateSubnet", is(true))));
    }

    @Test
    public void testWhenOnePublicSubnetWithNoPublicIpThenThrowsBadRequest() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp()
                .build();
        thrown.expect(BadRequestException.class);
        thrown.expectMessage("No suitable subnet found as there were neither private nor any suitable public subnets in 'subnet-1");

        underTest.selectInternal(subnets);
    }

    @Test
    public void testProperties() {
        assertEquals(1, underTest.getMinimumNumberOfSubnets());
        assertEquals(SubnetSelectorStrategyType.SINGLE_PREFER_PUBLIC, underTest.getType());
    }

}
