package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.SINGLE_RESOURCE_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneService;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateEndpointServicesProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
import com.sequenceiq.cloudbreak.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
public class AzureNewPrivateDnsZoneValidatorServiceTest {

    @Mock
    private AzurePrivateDnsZoneValidatorService azurePrivateDnsZoneValidatorService;

    @Mock
    private AzurePrivateEndpointServicesProvider azurePrivateEndpointServicesProvider;

    @InjectMocks
    private AzureNewPrivateDnsZoneValidatorService underTest;

    @Mock
    private AzureClient azureClient;

    private final List<AzureManagedPrivateDnsZoneService> availableServicesForPrivateEndpoint = Arrays.asList(AzureManagedPrivateDnsZoneService.values());

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE,
            names = {"POSTGRES_WITH_EXISTING_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_EXISTING_DNS_ZONE"})
    void testZonesNotConnectedToNetworkWhenNoExistingDnsZones(PrivateDatabaseVariant variant) {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone = Set.of();
        when(azurePrivateEndpointServicesProvider.getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone, variant))
                .thenReturn(availableServicesForPrivateEndpoint);
        List<PrivateDnsZone> privateDnsZoneList = setupPrivateDnsZones();

        underTest.zonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, servicesWithExistingPrivateDnsZone, variant, resultBuilder);

        ArgumentCaptor<String> dnsZoneNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(azurePrivateDnsZoneValidatorService, times(availableServicesForPrivateEndpoint.size())).privateDnsZonesNotConnectedToNetwork(eq(azureClient),
                eq(NETWORK_NAME), eq(SINGLE_RESOURCE_GROUP_NAME), dnsZoneNameArgumentCaptor.capture(), eq(resultBuilder), eq(privateDnsZoneList));
        List<String> checkedDnsZoneNames = dnsZoneNameArgumentCaptor.getAllValues();
        List<String> servicesNotChecked = availableServicesForPrivateEndpoint.stream()
                .map(azureManagedPrivateDnsZoneService -> azureManagedPrivateDnsZoneService.getDnsZoneName(SINGLE_RESOURCE_GROUP_NAME))
                .filter(dzn -> !checkedDnsZoneNames.contains(dzn))
                .collect(Collectors.toList());
        assertThat(servicesNotChecked).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE, names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_NEW_DNS_ZONE"})
    void testZonesNotConnectedToNetworkWhenNoCdpManagedZones(PrivateDatabaseVariant variant) {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone = new HashSet<>(availableServicesForPrivateEndpoint);
        when(azurePrivateEndpointServicesProvider.getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone, variant)).thenReturn(List.of());

        underTest.zonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, servicesWithExistingPrivateDnsZone, variant, resultBuilder);

        verify(azurePrivateDnsZoneValidatorService, never()).privateDnsZonesNotConnectedToNetwork(any(), any(), anyString(), anyString(), any(), any());
    }

    private List<PrivateDnsZone> setupPrivateDnsZones() {
        List<PrivateDnsZone> privateDnsZoneList = new ArrayList<>();
        AzureListResult<PrivateDnsZone> azureListResult = mock(AzureListResult.class);
        when(azureListResult.getAll()).thenReturn(privateDnsZoneList);
        when(azureClient.getPrivateDnsZoneList()).thenReturn(azureListResult);
        return privateDnsZoneList;
    }

}