package com.sequenceiq.environment.network.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class SubnetIdProviderTest {

    private static final String SUBNET_ID_1 = "subnetId1";

    private static final String SUBNET_ID_2 = "subnetId2";

    private static final String SUBNET_ID_3 = "subnetId3";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @InjectMocks
    private SubnetIdProvider underTest;

    @Test
    void testProvideThenNetworkSelectorCalled() {
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of("AZ-a", new CloudSubnet()))
                .build();
        NetworkConnector networkConnector = setupConnector();
        Tunnel tunnel = Tunnel.DIRECT;

        underTest.provide(networkDto, tunnel, CloudPlatform.AWS);

        ArgumentCaptor<SubnetSelectionParameters> subnetSelectionParametersCaptor = ArgumentCaptor.forClass(SubnetSelectionParameters.class);
        verify(networkConnector).selectSubnets(any(), subnetSelectionParametersCaptor.capture());
        assertFalse(subnetSelectionParametersCaptor.getValue().isForDatabase());
        assertFalse(subnetSelectionParametersCaptor.getValue().isHa());
        assertEquals(tunnel, subnetSelectionParametersCaptor.getValue().getTunnel());
    }

    @Test
    void testProvideShouldReturnNullWhenNetworkNull() {
        String actual = underTest.provide(null, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNull(actual);
    }

    @Test
    void testProvideShouldReturnNullWhenNoSubnetMetas() {
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of())
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNull(actual);
    }

    private NetworkConnector setupConnector() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        NetworkConnector networkConnector = mock(NetworkConnector.class);
        when(networkConnector.selectSubnets(any(), any()))
                .thenReturn(List.of(new CloudSubnet()));
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        return networkConnector;
    }
}