package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

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
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@RunWith(MockitoJUnitRunner.class)
public class SubnetSelectorStrategySinglePreferPrivateTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SubnetSelectorService subnetSelectorService;

    @InjectMocks
    private SubnetSelectorStrategySinglePreferPrivate underTest;

    private final SubnetHelper subnetHelper = new SubnetHelper();

    @Before
    public void setup() {
        when(subnetSelectorService.getOnePrivateSubnet(ArgumentMatchers.any())).thenCallRealMethod();
        when(subnetSelectorService.getOnePublicSubnet(ArgumentMatchers.any())).thenCallRealMethod();
    }

    @Test
    public void testWhenOnePrivateSubnetThenPrivateReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet()
                .build();

        SubnetSelectionResult chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets.getResult(), hasSize(1));
        assertThat(chosenSubnets.getResult(), hasItem(hasProperty("privateSubnet", is(true))));
    }

    @Test
    public void testWhenOnePublicSubnetThenPublicReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet()
                .build();

        SubnetSelectionResult chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets.getResult(), hasSize(1));
        assertThat(chosenSubnets.getResult(), hasItem(hasProperty("privateSubnet", is(false))));
    }

    @Test
    public void testWhenOnePrivateAndOnePublicSubnetThenPrivateReturned() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet()
                .withPublicSubnet()
                .build();

        SubnetSelectionResult chosenSubnets = underTest.selectInternal(subnets);

        assertThat(chosenSubnets.getResult(), hasSize(1));
        assertThat(chosenSubnets.getResult(), hasItem(hasProperty("privateSubnet", is(true))));
    }

    @Test
    public void testWhenPublicSubnetWithNoPublicIpThenErrorMessage() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnetNoPublicIp(AZ_A)
                .build();

        SubnetSelectionResult result = underTest.selectInternal(subnets);

        assertTrue(result.hasError());
        assertEquals("No suitable subnet found as there were neither private nor any suitable public subnets in 'subnet-1'.", result.getErrorMessage());
    }

    @Test
    public void testProperties() {
        assertEquals(1, underTest.getMinimumNumberOfSubnets());
        assertEquals(SubnetSelectorStrategyType.SINGLE_PREFER_PRIVATE, underTest.getType());
    }
}
