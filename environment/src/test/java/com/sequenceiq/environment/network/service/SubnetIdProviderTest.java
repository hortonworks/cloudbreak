package com.sequenceiq.environment.network.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

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
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.service.domain.ProvidedSubnetIds;

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
        NetworkConnector networkConnector = setupConnectorWithSelectionResult(List.of(
                new CloudSubnet.Builder()
                        .id("id")
                        .name("name")
                        .build()));
        Tunnel tunnel = Tunnel.DIRECT;

        ProvidedSubnetIds providedSubnetIds = underTest.subnets(networkDto, tunnel, CloudPlatform.AWS, false);

        assertEquals("id", providedSubnetIds.getSubnetId());
        ArgumentCaptor<SubnetSelectionParameters> subnetSelectionParametersCaptor = ArgumentCaptor.forClass(SubnetSelectionParameters.class);

        verify(networkConnector).chooseSubnets(any(), subnetSelectionParametersCaptor.capture());
        assertFalse(subnetSelectionParametersCaptor.getValue().isPreferPrivateIfExist());
        assertFalse(subnetSelectionParametersCaptor.getValue().isHa());
        assertEquals(tunnel, subnetSelectionParametersCaptor.getValue().getTunnel());
    }

    @Test
    void testProvideShouldReturnNullWhenNetworkNull() {
        ProvidedSubnetIds actual = underTest.subnets(null, Tunnel.DIRECT, CloudPlatform.AWS, false);

        assertNull(actual);
    }

    @Test
    void testProvideShouldReturnNullWhenNoSubnetMetas() {
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of())
                .build();

        ProvidedSubnetIds providedSubnetIds = underTest.subnets(networkDto, Tunnel.DIRECT, CloudPlatform.AWS, false);

        assertNull(providedSubnetIds);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultHasError() {
        setupConnectorWithSelectionError("error message");
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()))
                .withSubnetMetas(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()
                ))
                .build();

        ProvidedSubnetIds providedSubnetIds = underTest.subnets(networkDto, Tunnel.DIRECT, CloudPlatform.AWS, false);

        assertNotNull(providedSubnetIds);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultIsEmptyAndNoError() {
        setupConnectorWithSelectionResult(List.of());
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()))
                .withSubnetMetas(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()
                ))
                .build();

        ProvidedSubnetIds providedSubnetIds = underTest.subnets(networkDto, Tunnel.DIRECT, CloudPlatform.AWS, false);

        assertNotNull(providedSubnetIds);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultIsNotEmptyAndNoErrorButAtLeastTwoSubnetComesBackAsResult() {
        List<CloudSubnet> subnets = List.of(
                new CloudSubnet.Builder()
                        .id("id-1")
                        .name("name-1")
                        .build(),
                new CloudSubnet.Builder()
                        .id("id-2")
                        .name("name-2")
                        .build()
        );

        setupConnectorWithSelectionResult(subnets);
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()
                ))
                .withCbSubnets(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()
                ))
                .build();

        ProvidedSubnetIds providedSubnetIds = underTest.subnets(networkDto, Tunnel.DIRECT, CloudPlatform.AWS, false);

        assertNotNull(providedSubnetIds);
    }

    @Test
    public void shouldReturnNullInCaseOfNonSupportedCloudPlatform() {
        setupNotSupportedConnector();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()
                ))
                .withCbSubnets(Map.of(
                        "AZ-a",
                        new CloudSubnet.Builder()
                                .id("id-1")
                                .name("name-1")
                                .build(),
                        "AZ-b",
                        new CloudSubnet.Builder()
                                .id("id-2")
                                .name("name-2")
                                .build()
                ))
                .build();

        ProvidedSubnetIds providedSubnetIds = underTest.subnets(networkDto, Tunnel.DIRECT, CloudPlatform.AWS, false);

        assertNull(providedSubnetIds);
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
        when(networkConnector.chooseSubnets(any(), any()))
                .thenReturn(subnetSelectionResult);
        when(cloudConnector.networkConnector()).thenReturn(networkConnector);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
        return networkConnector;
    }

    private void setupNotSupportedConnector() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
    }
}