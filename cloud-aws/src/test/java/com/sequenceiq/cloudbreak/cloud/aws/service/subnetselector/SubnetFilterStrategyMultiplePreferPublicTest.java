package com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector;

import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_A;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_B;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_C;
import static com.sequenceiq.cloudbreak.cloud.aws.service.subnetselector.SubnetBuilder.AZ_D;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
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

import com.sequenceiq.cloudbreak.cloud.aws.service.SubnetCollectorService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@RunWith(MockitoJUnitRunner.class)
public class SubnetFilterStrategyMultiplePreferPublicTest {

    @Mock
    private SubnetCollectorService subnetCollectorService;

    @InjectMocks
    private SubnetFilterStrategyMultiplePreferPublic underTest;

    @Before
    public void setup() {
        when(subnetCollectorService.collectPublicSubnets(ArgumentMatchers.any())).thenCallRealMethod();
        when(subnetCollectorService.collectPrivateSubnets(ArgumentMatchers.any())).thenCallRealMethod();
    }

    @Test
    public void testSelectHAPublicSubnetWhenOnlyPublicSubnetPresentedShouldReturnPublicSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, false);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testSelectHAPublicSubnetWhenMixedSubnetPresentedInInternalTenantShouldReturnMixedSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, true);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testSelectHAPublicSubnetWhenMixedSubnetPresentedInExternalTenantShouldReturnMixedSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, false);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        verify(subnetCollectorService, never()).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }

    @Test
    public void testSelectHAPublicSubnetWhenMixedSubnetPresentedInInternalTenantShouldReturnMixedSubnetsWithNotUniquePublicAzs() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, true);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(4));
    }

    @Test
    public void testSelectHAPublicSubnetWhenMixedSubnetPresentedInExternalTenantShouldReturnMixedSubnetsWithNotUniquePublicAzs() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, false);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        verify(subnetCollectorService, never()).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testSelectNonHAMixedSubnetWhenOnlyPrivateSubnetPresentedShouldReturnMixedSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPublicSubnet(AZ_A)
                .withPublicSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .withPrivateSubnet(AZ_C)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1, false);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
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

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1, false);

        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }
}
