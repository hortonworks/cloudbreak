package com.sequenceiq.environment.environment.validation.network.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneService.POSTGRES_FLEXIBLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneService;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRegisteredPrivateDnsZoneService;
import com.sequenceiq.cloudbreak.util.NullUtil;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
public class AzureExistingPrivateDnsZonesServiceTest {

    private static final String POSTGRES_PRIVATE_DNS_ZONE_ID = "postgresPrivateDnsZoneId";

    private static final String AKS_PRIVATE_DNS_ZONE_ID = "aksPrivateDnsZoneId";

    private static final String FLEXIBLE_SERVER_SUBNET_ID = "subnetId";

    private final AzureExistingPrivateDnsZonesService underTest = new AzureExistingPrivateDnsZonesService();

    @Test
    void testGetExistingManagedZonesWhenPostgresPresent() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Map<AzureManagedPrivateDnsZoneService, String> existingZones = underTest.getExistingManagedZones(networkDto);

        assertEquals(POSTGRES_PRIVATE_DNS_ZONE_ID, existingZones.get(AzureManagedPrivateDnsZoneService.POSTGRES));
    }

    @Test
    void testGetExistingManagedZonesWhenPostgresNotPresent() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Map<AzureManagedPrivateDnsZoneService, String> existingZones = underTest.getExistingManagedZones(networkDto);

        assertThat(existingZones).isEmpty();
    }

    @Test
    void testGetExistingManagedZonesAsDescriptorsWhenPostgresPresent() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Map<AzurePrivateDnsZoneDescriptor, String> existingZones = underTest.getExistingManagedZonesAsDescriptors(networkDto);

        assertEquals(POSTGRES_PRIVATE_DNS_ZONE_ID, existingZones.get(AzureManagedPrivateDnsZoneService.POSTGRES));
    }

    @Test
    void testGetExistingManagedZonesAsDescriptorsWhenFlexiblePostgresPresent() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null, FLEXIBLE_SERVER_SUBNET_ID);

        Map<AzurePrivateDnsZoneDescriptor, String> existingZones = underTest.getExistingManagedZonesAsDescriptors(networkDto);

        assertEquals(POSTGRES_PRIVATE_DNS_ZONE_ID, existingZones.get(POSTGRES_FLEXIBLE));
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

        assertEquals(AKS_PRIVATE_DNS_ZONE_ID, existingZones.get(AzureRegisteredPrivateDnsZoneService.AKS));
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

        Set<AzureManagedPrivateDnsZoneService> servicesWithPrivateDnsZones = underTest.getServicesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).hasSize(1);
        assertThat(servicesWithPrivateDnsZones).contains(AzureManagedPrivateDnsZoneService.POSTGRES);
    }

    @Test
    void testGetServicesWithExistingManagedZonesWhenNoServicesWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(null, null);

        Set<AzureManagedPrivateDnsZoneService> servicesWithPrivateDnsZones = underTest.getServicesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).isEmpty();
    }

    @Test
    void testGetServiceNamesWithExistingManagedZonesWhenPostgresWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(POSTGRES_PRIVATE_DNS_ZONE_ID, null);

        Set<String> servicesWithPrivateDnsZones = underTest.getServiceNamesWithExistingManagedZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).hasSize(1);
        assertThat(servicesWithPrivateDnsZones).contains(AzureManagedPrivateDnsZoneService.POSTGRES.name());
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
        return getNetworkDto(postgresPrivateDnsZoneId, aksPrivateDnsZoneId, null);
    }

    private NetworkDto getNetworkDto(String postgresPrivateDnsZoneId, String aksPrivateDnsZoneId, String flexibleSubnetId) {
        return NetworkDto.builder()
                .withAzure(
                        AzureParams.builder()
                                .withDatabasePrivateDnsZoneId(postgresPrivateDnsZoneId)
                                .withAksPrivateDnsZoneId(aksPrivateDnsZoneId)
                                .withFlexibleServerSubnetIds(NullUtil.getIfNotNullOtherwise(flexibleSubnetId, Set::of, null))
                                .build()
                )
                .build();
    }

}
