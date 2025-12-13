package com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.common.subnetselector.SubnetBuilder.AZ_D;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@ExtendWith(MockitoExtension.class)
public class SubnetFilterStrategyMultiplePreferPrivateTest {

    @Mock
    private SubnetSelectorService subnetSelectorService;

    @InjectMocks
    private SubnetFilterStrategyMultiplePreferPrivate underTest;

    @Test
    public void testFilterHAMixedSubnetWhenOnlyPrivateSubnetHasUniqueAZPresentedShouldReturnOnlyPrivateSubnets() {
        when(subnetSelectorService.collectPrivateSubnets(any())).thenCallRealMethod();
        when(subnetSelectorService.collectPublicSubnets(any())).thenCallRealMethod();
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3);

        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }

    @Test
    public void testFilterHAPrivateSubnetWhenMixedSubnetWithDifferentAzPresentedShouldReturnPrivateAnPublicSubnets() {
        when(subnetSelectorService.collectPrivateSubnets(any())).thenCallRealMethod();
        when(subnetSelectorService.collectPublicSubnets(any())).thenCallRealMethod();
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3);

        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        verify(subnetSelectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testFilterNonHAPrivateSubnetWhenOnlyPrivateSubnetPresentedShouldReturnPrivateSubnets() {
        when(subnetSelectorService.collectPrivateSubnets(any())).thenCallRealMethod();
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1);

        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testFilterNonHAPrivateSubnetWhenMixedSubnetWithDifferentAzPresentedShouldReturnPrivateAnPublicSubnets() {
        when(subnetSelectorService.collectPrivateSubnets(any())).thenCallRealMethod();
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1);

        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }

    @Test
    public void testFilterPrivateSubnetWhenOnlyPrivateAndPublicSubnetPresentedShouldReturnPrivateSubnets() {
        when(subnetSelectorService.collectPrivateSubnets(any())).thenCallRealMethod();
        when(subnetSelectorService.collectPublicSubnets(any())).thenCallRealMethod();
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3);

        verify(subnetSelectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(4));
    }

}
