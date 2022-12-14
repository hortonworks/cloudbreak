package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.A_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_RESOURCE_ID;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.SINGLE_RESOURCE_GROUP_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.ZONE_NAME_POSTGRES;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.getPrivateDnsZoneResourceId;
import static com.sequenceiq.cloudbreak.cloud.model.filesystem.CloudAdlsView.SUBSCRIPTION_ID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.SubResource;
import com.microsoft.azure.arm.resources.ResourceId;
import com.microsoft.azure.management.network.Network;
import com.microsoft.azure.management.privatedns.v2018_09_01.PrivateZone;
import com.microsoft.azure.management.privatedns.v2018_09_01.ProvisioningState;
import com.microsoft.azure.management.privatedns.v2018_09_01.implementation.VirtualNetworkLinkInner;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.validator.ValidationTestUtil;
import com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.TestPagedList;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
public class AzurePrivateDnsZoneValidatorServiceTest {

    private static final String UNKNOWN_SERVICE_ZONE_NAME = "unknown.service.zone.name";

    @Mock
    private AzurePrivateDnsZoneMatcherService azurePrivateDnsZoneMatcherService;

    @InjectMocks
    private AzurePrivateDnsZoneValidatorService underTest;

    @Mock
    private AzureClient azureClient;

    @Test
    void testExistingPrivateDnsZoneNamesAreSupportedWhenSupportedDnsZoneName() {
        ResourceId privateDnsZoneResourceId = getPrivateDnsZoneResourceId();
        when(azurePrivateDnsZoneMatcherService.isZoneNameMatchingPattern(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneResourceId.name()))
                .thenReturn(true);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        resultBuilder = underTest.existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZoneResourceId, resultBuilder);

        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    void testExistingPrivateDnsZoneNamesAreSupportedWhenUnsupportedDnsZoneName() {
        when(azurePrivateDnsZoneMatcherService.isZoneNameMatchingPattern(AzurePrivateDnsZoneServiceEnum.POSTGRES, UNKNOWN_SERVICE_ZONE_NAME))
                .thenReturn(false);
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();

        resultBuilder = underTest.existingPrivateDnsZoneNameIsSupported(AzurePrivateDnsZoneServiceEnum.POSTGRES,
                getPrivateDnsZoneResourceId(A_RESOURCE_GROUP_NAME, UNKNOWN_SERVICE_ZONE_NAME), resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of(
                "The provided private DNS zone /subscriptions/subscriptionId/resourceGroups/a-resource-group-name/providers/Microsoft.Network/privateDnsZones" +
                        "/unknown.service.zone.name is not a valid DNS zone name for Microsoft.DBforPostgreSQL/servers. Please use a DNS zone with name " +
                        "privatelink.postgres.database.azure.com and try again."));
    }

    @Test
    void testPrivateDnsZoneExistsWhenZoneExists() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateZones = new TestPagedList<>();
        privateZones.add(getPrivateDnsZone(null, ZONE_NAME_POSTGRES, null));
        when(azureClient.getPrivateDnsZonesByResourceGroup(SUBSCRIPTION_ID, SINGLE_RESOURCE_GROUP_NAME)).thenReturn(privateZones);

        resultBuilder = underTest.privateDnsZoneExists(azureClient, getPrivateDnsZoneResourceId(), resultBuilder);

        ValidationResult result = resultBuilder.build();
        assertFalse(result.hasError());
    }

    @Test
    void testPrivateDnsZoneExistsWhenZoneDoesNotExist() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateZones = new TestPagedList<>();
        when(azureClient.getPrivateDnsZonesByResourceGroup(SUBSCRIPTION_ID, A_RESOURCE_GROUP_NAME)).thenReturn(privateZones);

        resultBuilder = underTest.privateDnsZoneExists(azureClient, getPrivateDnsZoneResourceId(A_RESOURCE_GROUP_NAME), resultBuilder);

        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of(
                "The provided private DNS zone /subscriptions/subscriptionId/resourceGroups/a-resource-group-name/providers/Microsoft.Network/privateDnsZones" +
                        "/privatelink.postgres.database.azure.com does not exist. Please make sure the specified private DNS zone exists and try environment " +
                        "creation again."));
    }

    @Test
    void testPrivateDnsZoneConnectedToNetworkWhenConnectedToEnvironmentNetwork() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        Network network = getNetwork();
        when(azureClient.getNetworkByResourceGroup(NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME)).thenReturn(network);
        PagedList<VirtualNetworkLinkInner> virtualNetworkLinks = getNetworkLinks(List.of(NETWORK_RESOURCE_ID));
        when(azureClient.listNetworkLinksByPrivateDnsZoneName(SUBSCRIPTION_ID, A_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES)).thenReturn(virtualNetworkLinks);

        resultBuilder = underTest.privateDnsZoneConnectedToNetwork(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME,
                getPrivateDnsZoneResourceId(A_RESOURCE_GROUP_NAME), resultBuilder);

        assertFalse(resultBuilder.build().hasError());
    }

    @Test
    void testPrivateDnsZoneConnectedToNetworkWhenNotConnectedToEnvironmentNetwork() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        Network network = getNetwork();
        when(azureClient.getNetworkByResourceGroup(NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME)).thenReturn(network);
        PagedList<VirtualNetworkLinkInner> virtualNetworkLinks = getNetworkLinks(List.of("anotherNetwork"));
        when(azureClient.listNetworkLinksByPrivateDnsZoneName(SUBSCRIPTION_ID, A_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES)).thenReturn(virtualNetworkLinks);

        resultBuilder = underTest.privateDnsZoneConnectedToNetwork(azureClient, NETWORK_RESOURCE_GROUP_NAME, NETWORK_NAME,
                getPrivateDnsZoneResourceId(A_RESOURCE_GROUP_NAME), resultBuilder);

        assertTrue(resultBuilder.build().hasError());
        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of(
                "The private DNS zone /subscriptions/subscriptionId/resourceGroups/a-resource-group-name/providers/Microsoft.Network/privateDnsZones" +
                        "/privatelink.postgres.database.azure.com does not have a network link to network networkName. Please make sure the private DNS zone " +
                        "is connected to the network provided to the environment."));
    }

    @Test
    void testPrivateDnsZonesNotConnectedToNetworkWhenNoZonesFound() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateDnsZoneList = new TestPagedList<>();

        ValidationResult result = underTest.privateDnsZonesNotConnectedToNetwork(azureClient, NETWORK_NAME, A_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES,
                resultBuilder, privateDnsZoneList);

        assertFalse(result.hasError());
        verify(azureClient, never()).getNetworkLinkByPrivateDnsZone(anyString(), anyString(), anyString());
    }

    @Test
    void testPrivateDnsZonesNotConnectedToNetworkWhenZoneInSingleRG() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateDnsZoneList = getPrivateDnsZones(SINGLE_RESOURCE_GROUP_NAME, List.of(""), null);

        ValidationResult result = underTest.privateDnsZonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES,
                resultBuilder, privateDnsZoneList);

        assertFalse(result.hasError());
        verify(azureClient, never()).getNetworkLinkByPrivateDnsZone(anyString(), anyString(), anyString());
    }

    @Test
    void testPrivateDnsZonesNotConnectedToNetworkWhenZoneNameIsNotSupported() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateDnsZoneList = getPrivateDnsZones(A_RESOURCE_GROUP_NAME, List.of("unrelated.zone"), null);

        ValidationResult result = underTest.privateDnsZonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES,
                resultBuilder, privateDnsZoneList);

        assertFalse(result.hasError());
        verify(azureClient, never()).getNetworkLinkByPrivateDnsZone(anyString(), anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource(value = "getAllProvisioningStates")
    void testPrivateDnsZonesNotConnectedToNetworkWhenZoneProvisioningStateNotSucceeded(ProvisioningState provisioningState) {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateDnsZoneList = getPrivateDnsZones(A_RESOURCE_GROUP_NAME, List.of(ZONE_NAME_POSTGRES), provisioningState);

        ValidationResult result = underTest.privateDnsZonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES,
                resultBuilder, privateDnsZoneList);

        assertFalse(result.hasError());
        verify(azureClient, never()).getNetworkLinkByPrivateDnsZone(anyString(), anyString(), anyString());
    }

    @Test
    void testPrivateDnsZonesNotConnectedToNetworkWhenZoneNotConnected() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateDnsZoneList = getPrivateDnsZones(A_RESOURCE_GROUP_NAME, List.of(ZONE_NAME_POSTGRES), ProvisioningState.SUCCEEDED);
        when(azureClient.getNetworkLinkByPrivateDnsZone(A_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES, NETWORK_NAME)).thenReturn(null);

        ValidationResult result = underTest.privateDnsZonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES,
                resultBuilder, privateDnsZoneList);

        assertFalse(result.hasError());
    }

    @Test
    void testPrivateDnsZonesNotConnectedToNetworkWhenZoneConnected() {
        ValidationResult.ValidationResultBuilder resultBuilder = ValidationResult.builder();
        PagedList<PrivateZone> privateDnsZoneList = getPrivateDnsZones(A_RESOURCE_GROUP_NAME, List.of(ZONE_NAME_POSTGRES), ProvisioningState.SUCCEEDED);
        when(azureClient.getNetworkLinkByPrivateDnsZone(A_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES, NETWORK_NAME)).thenReturn(new VirtualNetworkLinkInner());

        ValidationResult result = underTest.privateDnsZonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, ZONE_NAME_POSTGRES,
                resultBuilder, privateDnsZoneList);

        assertTrue(result.hasError());
        ValidationTestUtil.checkErrorsPresent(resultBuilder, List.of("Network link for the network networkName already exists for Private DNS Zone " +
                "privatelink.postgres.database.azure.com in resource group a-resource-group-name. Please ensure that there is no existing network link and " +
                "try again!"));
    }

    private static List<ProvisioningState> getAllProvisioningStates() {
        return ProvisioningState.values().stream()
                .filter(ps -> !ps.equals(ProvisioningState.SUCCEEDED))
                .collect(Collectors.toList());
    }

    private PagedList<VirtualNetworkLinkInner> getNetworkLinks(List<String> foundNetworkIds) {
        PagedList<VirtualNetworkLinkInner> virtualNetworkLinks = new TestPagedList<>();
        foundNetworkIds.stream()
                .map(id -> new VirtualNetworkLinkInner().withVirtualNetwork(new SubResource().withId(id)))
                .forEach(virtualNetworkLinks::add);
        return virtualNetworkLinks;
    }

    private Network getNetwork() {
        Network network = mock(Network.class);
        when(network.id()).thenReturn(NETWORK_RESOURCE_ID);
        return network;
    }

    private PagedList<PrivateZone> getPrivateDnsZones(String resourceGroupName, List<String> privateZoneNames, ProvisioningState provisioningState) {
        PagedList<PrivateZone> privateZones = new TestPagedList<>();
        privateZoneNames.stream()
                .map(pzn -> getPrivateDnsZone(resourceGroupName, pzn, provisioningState))
                .forEach(privateZones::add);
        return privateZones;
    }

    private PrivateZone getPrivateDnsZone(String resourceGroupName, String dnsZoneName, ProvisioningState provisioningState) {
        PrivateZone privateZone = mock(PrivateZone.class);
        if (StringUtils.isNotEmpty(dnsZoneName)) {
            when(privateZone.name()).thenReturn(dnsZoneName);
        }
        if (StringUtils.isNotEmpty(resourceGroupName)) {
            when(privateZone.resourceGroupName()).thenReturn(resourceGroupName);
        }
        if (provisioningState != null) {
            when(privateZone.provisioningState()).thenReturn(provisioningState);
        }
        return privateZone;
    }

}