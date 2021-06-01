package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_D;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@RunWith(MockitoJUnitRunner.class)
public class SubnetFilterStrategyMultiplePreferPublicTest {

    @Mock
    private SubnetSelectorService subnetSelectorService;

    @InjectMocks
    private SubnetFilterStrategyMultiplePreferPublic underTest;

    @Before
    public void setup() {
        when(subnetSelectorService.collectPublicSubnets(ArgumentMatchers.any())).thenCallRealMethod();
        when(subnetSelectorService.collectPrivateSubnets(ArgumentMatchers.any())).thenCallRealMethod();
    }

    @Test
    public void testSelectHAPublicSubnetWhenOnlyPublicSubnetPresentedShouldReturnPublicSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3);

        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testSelectHAPublicSubnetWhenMixedSubnetPresentedShouldReturnMixedSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3);

        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testSelectHAPublicSubnetWhenMixedSubnetPresentedShouldReturnMixedSubnetsWithNotUniquePublicAzs() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3);

        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(4));
    }

    @Test
    public void testSelectNonHAMixedSubnetWhenOnlyPrivateSubnetPresentedShouldReturnMixedSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1);

        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testSelectNonHAPrivateSubnetWhenOnlyPrivateAndPublicSubnetPresentedShouldReturnPrivateSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1);

        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }
}
