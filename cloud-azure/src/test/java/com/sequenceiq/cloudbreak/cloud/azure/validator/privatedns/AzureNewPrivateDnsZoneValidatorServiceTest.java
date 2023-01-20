package com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns;

import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.NETWORK_NAME;
import static com.sequenceiq.cloudbreak.cloud.azure.validator.privatedns.PrivateDnsZoneValidationTestConstants.SINGLE_RESOURCE_GROUP_NAME;
import static org.assertj.core.api.Assertions.assertThat;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.resourcemanager.privatedns.models.PrivateDnsZone;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateEndpointServicesProvider;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureListResult;
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

    private final List<AzurePrivateDnsZoneServiceEnum> availableServicesForPrivateEndpoint = Arrays.asList(AzurePrivateDnsZoneServiceEnum.values());

    @Test
    void testZonesNotConnectedToNetworkWhenNoExistingDnsZones() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingPrivateDnsZone = Set.of();
        when(azurePrivateEndpointServicesProvider.getCdpManagedDnsZones(servicesWithExistingPrivateDnsZone)).thenReturn(availableServicesForPrivateEndpoint);
        List<PrivateDnsZone> privateDnsZoneList = setupPrivateDnsZones();

        underTest.zonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, servicesWithExistingPrivateDnsZone, resultBuilder);

        ArgumentCaptor<String> dnsZoneNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(azurePrivateDnsZoneValidatorService, times(availableServicesForPrivateEndpoint.size())).privateDnsZonesNotConnectedToNetwork(eq(azureClient),
                eq(NETWORK_NAME), eq(SINGLE_RESOURCE_GROUP_NAME), dnsZoneNameArgumentCaptor.capture(), eq(resultBuilder), eq(privateDnsZoneList));
        List<String> checkedDnsZoneNames = dnsZoneNameArgumentCaptor.getAllValues();
        List<String> servicesNotChecked = availableServicesForPrivateEndpoint.stream()
                .map(AzurePrivateDnsZoneServiceEnum::getDnsZoneName)
                .filter(dzn -> !checkedDnsZoneNames.contains(dzn))
                .collect(Collectors.toList());
        assertThat(servicesNotChecked).isEmpty();
    }

    @Test
    void testZonesNotConnectedToNetworkWhenNoCdpManagedZones() {
        ValidationResult.ValidationResultBuilder resultBuilder = new ValidationResult.ValidationResultBuilder();
        Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingPrivateDnsZone = new HashSet<>(availableServicesForPrivateEndpoint);
        when(azurePrivateEndpointServicesProvider.getCdpManagedDnsZones(servicesWithExistingPrivateDnsZone)).thenReturn(List.of());

        underTest.zonesNotConnectedToNetwork(azureClient, NETWORK_NAME, SINGLE_RESOURCE_GROUP_NAME, servicesWithExistingPrivateDnsZone, resultBuilder);

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