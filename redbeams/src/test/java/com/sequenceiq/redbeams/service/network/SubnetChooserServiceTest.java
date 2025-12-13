package com.sequenceiq.redbeams.service.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@ExtendWith(MockitoExtension.class)
class SubnetChooserServiceTest {

    private static final String AVAILABILITY_ZONE_A = "AZ-a";

    private static final String AVAILABILITY_ZONE_B = "AZ-b";

    private static final String SUBNET_1 = "subnet-1";

    private static final String SUBNET_2 = "subnet-2";

    private static final String SUBNET_3 = "subnet-3";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private SubnetChooserService underTest;

    @Test
    void testChooseSubnetsThenCloudPlatformConnectorGetIsCalled() {
        List<CloudSubnet> subnets = List.of();
        setupConnector();
        DBStack dbStack = new DBStack();
        dbStack.setCloudPlatform("MyCloudPlatform");
        dbStack.setPlatformVariant("MyPlatformVariant");

        underTest.chooseSubnets(subnets, dbStack);

        ArgumentCaptor<CloudPlatformVariant> cloudPlatformVariantArgumentCaptor = ArgumentCaptor.forClass(CloudPlatformVariant.class);
        verify(cloudPlatformConnectors).get(cloudPlatformVariantArgumentCaptor.capture());
        assertEquals("MyCloudPlatform", cloudPlatformVariantArgumentCaptor.getValue().getPlatform().value());
        assertEquals("MyPlatformVariant", cloudPlatformVariantArgumentCaptor.getValue().getVariant().value());
    }

    @Test
    void testChooseSubnetsThenNetworkSelectorCalled() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet.Builder()
                        .id(SUBNET_1)
                        .availabilityZone(AVAILABILITY_ZONE_A)
                        .build(),
                new CloudSubnet.Builder()
                        .id(SUBNET_2)
                        .availabilityZone(AVAILABILITY_ZONE_B)
                        .build(),
                new CloudSubnet.Builder()
                        .id(SUBNET_3)
                        .availabilityZone(AVAILABILITY_ZONE_B)
                        .build()
        );
        NetworkConnector networkConnector = setupConnector();

        underTest.chooseSubnets(subnets, new DBStack());

        ArgumentCaptor<SubnetSelectionParameters> subnetSelectionParametersCaptor = ArgumentCaptor.forClass(SubnetSelectionParameters.class);
        verify(networkConnector).chooseSubnets(eq(subnets), subnetSelectionParametersCaptor.capture());
        assertTrue(subnetSelectionParametersCaptor.getValue().isPreferPrivateIfExist());
        assertTrue(subnetSelectionParametersCaptor.getValue().isHa());
    }

    @Test
    void testChooseSubnetsWhenAwsNoHaThenHaIsFalse() {
        List<CloudSubnet> subnets = List.of();
        NetworkConnector networkConnector = setupConnector();
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.isHa()).thenReturn(false);

        underTest.chooseSubnets(subnets, dbStack);

        ArgumentCaptor<SubnetSelectionParameters> subnetSelectionParametersCaptor = ArgumentCaptor.forClass(SubnetSelectionParameters.class);
        verify(networkConnector).chooseSubnets(eq(subnets), subnetSelectionParametersCaptor.capture());
        assertTrue(subnetSelectionParametersCaptor.getValue().isPreferPrivateIfExist());
        assertFalse(subnetSelectionParametersCaptor.getValue().isHa());
    }

    @Test
    void testChooseSubnetsWhenAwsIsHaThenHaIsTrue() {
        List<CloudSubnet> subnets = List.of();
        NetworkConnector networkConnector = setupConnector();
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.isHa()).thenReturn(true);

        underTest.chooseSubnets(subnets, dbStack);

        ArgumentCaptor<SubnetSelectionParameters> subnetSelectionParametersCaptor = ArgumentCaptor.forClass(SubnetSelectionParameters.class);
        verify(networkConnector).chooseSubnets(eq(subnets), subnetSelectionParametersCaptor.capture());
        assertTrue(subnetSelectionParametersCaptor.getValue().isPreferPrivateIfExist());
        assertTrue(subnetSelectionParametersCaptor.getValue().isHa());
    }

    @Test
    void testChooseSubnetsWhenSubnetChooserReturnsErrorThenThrows() {
        List<CloudSubnet> subnets = List.of();
        setupConnector("my error message");
        DBStack dbStack = mock(DBStack.class);
        when(dbStack.isHa()).thenReturn(true);

        assertThrows(BadRequestException.class, () -> underTest.chooseSubnets(subnets, dbStack), "my error message");
    }

    private NetworkConnector setupConnector() {
        return setupConnector(null);
    }

    private NetworkConnector setupConnector(String errorMessage) {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        NetworkConnector networkConnector = mock(NetworkConnector.class);
        SubnetSelectionResult subnetSelectionResult = StringUtils.isEmpty(errorMessage)
                ? new SubnetSelectionResult(List.of())
                : new SubnetSelectionResult(errorMessage);
        when(networkConnector.chooseSubnets(any(), any())).thenReturn(subnetSelectionResult);
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        return networkConnector;
    }
}
