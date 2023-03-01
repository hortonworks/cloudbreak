package com.sequenceiq.environment.environment.validation.network.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneRegistrationEnum;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
public class AzureExistingPrivateDnsZonesServiceTest {

    private static final String POSTGRES_PRIVATE_DNS_ZONE_ID = "postgresPrivateDnsZoneId";

    private static final String AKS_PRIVATE_DNS_ZONE_ID = "aksPrivateDnsZoneId";

    private final AzureExistingPrivateDnsZonesService underTest = new AzureExistingPrivateDnsZonesService();

    @Test
    void testGetExistingManagedZonesWhenPostgresPresent() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Map<AzurePrivateDnsZoneServiceEnum, String> existingZones = underTest.getExistingManagedZones(networkDto);

        assertEquals(POSTGRES_PRIVATE_DNS_ZONE_ID, existingZones.get(AzurePrivateDnsZoneServiceEnum.POSTGRES));
    }

    @Test
    void testGetExistingManagedZonesWhenPostgresNotPresent() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Map<AzurePrivateDnsZoneServiceEnum, String> existingZones = underTest.getExistingManagedZones(networkDto);

        assertThat(existingZones).isEmpty();
    }

    @Test
    void testGetExistingManagedZonesAsDescriptorsWhenPostgresPresent() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Map<AzurePrivateDnsZoneDescriptor, String> existingZones = underTest.getExistingManagedZonesAsDescriptors(networkDto);

        assertEquals(POSTGRES_PRIVATE_DNS_ZONE_ID, existingZones.get(AzurePrivateDnsZoneServiceEnum.POSTGRES));
    }

    @Test
    void testGetExistingManagedZonesAsDescriptorsWhenPostgresNotPresent() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Map<AzurePrivateDnsZoneDescriptor, String> existingZones = underTest.getExistingManagedZonesAsDescriptors(networkDto);

        assertThat(existingZones).isEmpty();
    }

    @Test
    void testGetExistingRegisteredOnlyZonesAsDescriptorsWhenAksPresent() {
        NetworkDto networkDto = getNetworkDto(null, AKS_PRIVATE_DNS_ZONE_ID);

        Map<AzurePrivateDnsZoneDescriptor, String> existingZones = underTest.getExistingRegisteredOnlyZonesAsDescriptors(networkDto);

        assertEquals(AKS_PRIVATE_DNS_ZONE_ID, existingZones.get(AzurePrivateDnsZoneRegistrationEnum.AKS));
    }

    @Test
    void testGetExistingRegisteredOnlyZonesAsDescriptorsWhenAksNotPresent() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Map<AzurePrivateDnsZoneDescriptor, String> existingZones = underTest.getExistingRegisteredOnlyZonesAsDescriptors(networkDto);

        assertThat(existingZones).isEmpty();
    }

    @Test
    void testGetServicesWithExistingManagedZonesWhenPostgresWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Set<AzurePrivateDnsZoneServiceEnum> servicesWithPrivateDnsZones = underTest.getServicesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).hasSize(1);
        assertThat(servicesWithPrivateDnsZones).contains(AzurePrivateDnsZoneServiceEnum.POSTGRES);
    }

    @Test
    void testGetServicesWithExistingManagedZonesWhenNoServicesWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Set<AzurePrivateDnsZoneServiceEnum> servicesWithPrivateDnsZones = underTest.getServicesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).isEmpty();
    }

    @Test
    void testGetServiceNamesWithExistingManagedZonesWhenPostgresWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Set<String> servicesWithPrivateDnsZones = underTest.getServiceNamesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).hasSize(1);
        assertThat(servicesWithPrivateDnsZones).contains(AzurePrivateDnsZoneServiceEnum.POSTGRES.name());
    }

    @Test
    void testGetServiceNamesWithExistingManagedZonesWhenNoServicesWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Set<String> servicesWithPrivateDnsZones = underTest.getServiceNamesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).isEmpty();
    }

    @Test
    void testGetExistingRegisteredOnlyZonesAsDescriptors() {
        NetworkDto networkDto = getNetworkDto(null, AKS_PRIVATE_DNS_ZONE_ID);

        Set<String> servicesWithPrivateDnsZones = underTest.getServiceNamesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).isEmpty();

    }

    private NetworkDto getNetworkDto(String postgresPrivateDnsZoneId, String aksPrivateDnsZoneId) {
        return NetworkDto.builder()
                .withAzure(
                        AzureParams.builder()
                                .withDatabasePrivateDnsZoneId(postgresPrivateDnsZoneId)
                                .withAksPrivateDnsZoneId(aksPrivateDnsZoneId)
                                .build()
                )
                .build();
    }

}
