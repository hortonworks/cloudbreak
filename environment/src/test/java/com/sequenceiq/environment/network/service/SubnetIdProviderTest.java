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

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
class SubnetIdProviderTest {

    private static final String SUBNET_ID_1 = "subnetId1";

    private static final String SUBNET_ID_2 = "subnetId2";

    private static final String SUBNET_ID_3 = "subnetId3";

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SubnetIdProvider underTest;

    @Test
    void testProvideThenNetworkSelectorCalled() {
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of("AZ-a", new CloudSubnet()))
                .withSubnetMetas(Map.of("AZ-a", new CloudSubnet()))
                .build();
        NetworkConnector networkConnector = setupConnectorWithSelectionResult(List.of(new CloudSubnet("id", "name")));
        EnvironmentDto environmentDto = createEnvironmentDto(networkDto);

        String provide = underTest.provide(environmentDto);

        assertEquals("id", provide);
        ArgumentCaptor<SubnetSelectionParameters> subnetSelectionParametersCaptor = ArgumentCaptor.forClass(SubnetSelectionParameters.class);

        verify(networkConnector).chooseSubnets(any(), subnetSelectionParametersCaptor.capture());
        assertFalse(subnetSelectionParametersCaptor.getValue().isPreferPrivateIfExist());
        assertFalse(subnetSelectionParametersCaptor.getValue().isHa());
        assertEquals(environmentDto.getExperimentalFeatures().getTunnel(), subnetSelectionParametersCaptor.getValue().getTunnel());
    }

    @Test
    void testProvideShouldReturnNullWhenNetworkNull() {
        EnvironmentDto environmentDto = createEnvironmentDto(null);
        String actual = underTest.provide(environmentDto);

        Assertions.assertNull(actual);
    }

    @Test
    void testProvideShouldReturnNullWhenNoSubnetMetas() {
        NetworkDto networkDto = NetworkDto.builder()
                .withCbSubnets(Map.of())
                .build();
        EnvironmentDto environmentDto = createEnvironmentDto(networkDto);

        String actual = underTest.provide(environmentDto);

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
        EnvironmentDto environmentDto = createEnvironmentDto(networkDto);

        String actual = underTest.provide(environmentDto);

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
        EnvironmentDto environmentDto = createEnvironmentDto(networkDto);

        String actual = underTest.provide(environmentDto);

        Assertions.assertNotNull(actual);
    }

    @Test
    void testProvideShouldReturnAnySubnetWhenResultIsNotEmptyAndNoErrorButAtLeastTwoSubnetComesBackAsResult() {
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
                .withCbSubnets(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")
                ))
                .build();
        EnvironmentDto environmentDto = createEnvironmentDto(networkDto);

        String actual = underTest.provide(environmentDto);

        Assertions.assertNotNull(actual);
    }

    @Test
    public void shouldReturnNullInCaseOfNonSupportedCloudPlatform() {
        setupNotSupportedConnector();
        NetworkDto networkDto = NetworkDto.builder()
                .withSubnetMetas(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")
                ))
                .withCbSubnets(Map.of(
                        "AZ-a", new CloudSubnet("id-1", "name-1"),
                        "AZ-b", new CloudSubnet("id-2", "name-2")
                ))
                .build();
        EnvironmentDto environmentDto = createEnvironmentDto(networkDto);

        String actual = underTest.provide(environmentDto);

        Assertions.assertNull(actual);
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

    private EnvironmentDto createEnvironmentDto(NetworkDto networkDto) {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setNetwork(networkDto);
        environmentDto.setExperimentalFeatures(ExperimentalFeatures.builder().withTunnel(Tunnel.DIRECT).build());
        environmentDto.setCloudPlatform("MOCK");
        return environmentDto;
    }
}