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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.aws.service.SubnetCollectorService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;

@RunWith(MockitoJUnitRunner.class)
public class SubnetFilterStrategyMultiplePreferPrivateTest {

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private SubnetCollectorService subnetCollectorService;

    @InjectMocks
    private SubnetFilterStrategyMultiplePreferPrivate underTest;

    @Before
    public void setup() {
        when(subnetCollectorService.collectPrivateSubnets(ArgumentMatchers.any())).thenCallRealMethod();
        when(subnetCollectorService.collectPublicSubnets(ArgumentMatchers.any())).thenCallRealMethod();
    }

    @Test
    public void testFilterHAMixedSubnetWhenOnlyPrivateSubnetHasUniqueAZPresentedShouldReturnOnlyPrivateSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_B)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, false);

        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }

    @Test
    public void testFilterHAPrivateSubnetWhenMixedSubnetWithDifferentAzPresentedShouldReturnPrivateAnPublicSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, false);

        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        verify(subnetCollectorService, times(1)).collectPublicSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testFilterNonHAPrivateSubnetWhenOnlyPrivateSubnetPresentedShouldReturnPrivateSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1, false);

        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(3));
    }

    @Test
    public void testFilterNonHAPrivateSubnetWhenMixedSubnetWithDifferentAzPresentedShouldReturnPrivateAnPublicSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 1, false);

        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(2));
    }

    @Test
    public void testFilterPrivateSubnetWhenOnlyPrivateAndPublicSubnetPresentedShouldReturnPrivateSubnets() {
        List<CloudSubnet> subnets = new SubnetBuilder()
                .withPrivateSubnet(AZ_A)
                .withPrivateSubnet(AZ_B)
                .withPrivateSubnet(AZ_B)
                .withPublicSubnet(AZ_C)
                .withPublicSubnet(AZ_D)
                .build();

        SubnetSelectionResult chosenSubnets = underTest.filter(subnets, 3, false);

        verify(subnetCollectorService, times(1)).collectPrivateSubnets(anyCollection());
        assertThat(chosenSubnets.getResult(), hasSize(4));
    }

}
