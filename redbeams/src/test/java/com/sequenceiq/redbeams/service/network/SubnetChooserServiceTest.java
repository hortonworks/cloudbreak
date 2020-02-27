package com.sequenceiq.redbeams.service.network;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.exception.BadRequestException;

@RunWith(MockitoJUnitRunner.class)
public class SubnetChooserServiceTest {

    private static final String AVAILABILITY_ZONE_A = "AZ-a";

    private static final String AVAILABILITY_ZONE_B = "AZ-b";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final String SUBNET_3 = "subnet-3";

    @Mock
    private AwsSubnetChooser awsSubnetChooser;

    @InjectMocks
    private  SubnetChooserService underTest;

    @Test
    public void testAwsSubnetChooserCalled() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "", AVAILABILITY_ZONE_A, ""),
                new CloudSubnet(SUBNET_2, "", AVAILABILITY_ZONE_B, ""),
                new CloudSubnet(SUBNET_3, "", AVAILABILITY_ZONE_B, "")
        );

        underTest.chooseSubnets(subnets, CloudPlatform.AWS, null);
        verify(awsSubnetChooser).chooseSubnets(subnets, null);
    }

    @Test
    public void testChooseSubnetsAzure() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, "")
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, CloudPlatform.AZURE, null);

        assertEquals(1, chosenSubnets.size());
        assertTrue(subnets.contains(chosenSubnets.get(0)));
    }

    @Test
    public void testChooseSubnetsAzureWhenThreeSubnetsThenAllThreeReturned() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet(SUBNET_1, ""),
                new CloudSubnet(SUBNET_2, ""),
                new CloudSubnet(SUBNET_3, "")
        );

        List<CloudSubnet> chosenSubnets = underTest.chooseSubnets(subnets, CloudPlatform.AZURE, null);

        assertEquals(3, chosenSubnets.size());
        List<String> chosenSubnetIds = chosenSubnets.stream().map(CloudSubnet::getId).collect(Collectors.toList());
        assertThat(chosenSubnetIds, hasItem(SUBNET_1));
        assertThat(chosenSubnetIds, hasItem(SUBNET_2));
        assertThat(chosenSubnetIds, hasItem(SUBNET_3));
    }

    @Test(expected = BadRequestException.class)
    public void testChooseSubnetsAzureValidation() {
        List<CloudSubnet> subnets = List.of();
        underTest.chooseSubnets(subnets, CloudPlatform.AZURE, null);
    }

}
