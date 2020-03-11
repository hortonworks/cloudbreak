package com.sequenceiq.environment.network.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
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
                .withCbSubnets(Map.of("AZ-a", new CloudSubnet()))
                .withSubnetMetas(Map.of("AZ-a", new CloudSubnet()))
                .build();
        NetworkConnector networkConnector = setupConnectorWithSelectionResult(List.of(new CloudSubnet("id", "name")));
        Tunnel tunnel = Tunnel.DIRECT;

        String provide = underTest.provide(networkDto, tunnel, CloudPlatform.AWS);

        assertEquals("id", provide);
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
                .withCbSubnets(Map.of())
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNull(actual);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultHasError() {
        setupConnectorWithSelectionError("error message");
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")))
                .withSubnetMetas(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")
                ))
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNotNull(actual);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultIsEmptyAndNoError() {
        setupConnectorWithSelectionResult(List.of());
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")))
                .withSubnetMetas(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")
                ))
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNotNull(actual);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultIsEmptyAndNoError1() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet("id-1", "name-1"),
                new CloudSubnet("id-2", "name-2")
        );

        setupConnectorWithSelectionResult(subnets);
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")
                ))
                .build();

        String actual = underTest.provide(networkDto, Tunnel.DIRECT, CloudPlatform.AWS);

        Assertions.assertNotNull(actual);
    }

    private NetworkConnector setupConnectorWithSelectionResult(List<CloudSubnet> selectedSubnets) {
        return setupConnector(null, selectedSubnets);
    }

    private NetworkConnector setupConnectorWithSelectionError(String errorMessage) {
        return setupConnector(errorMessage, null);
    }

    private NetworkConnector setupConnector(String errorMessage, List<CloudSubnet> selectedSubnets) {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        NetworkConnector networkConnector = mock(NetworkConnector.class);
        SubnetSelectionResult subnetSelectionResult = StringUtils.isEmpty(errorMessage)
                ? new SubnetSelectionResult(selectedSubnets)
                : new SubnetSelectionResult(errorMessage);
        when(networkConnector.selectSubnets(any(), any()))
                .thenReturn(subnetSelectionResult);
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        return networkConnector;
    }
}