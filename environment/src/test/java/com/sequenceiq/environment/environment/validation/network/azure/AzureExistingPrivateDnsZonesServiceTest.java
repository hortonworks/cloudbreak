package com.sequenceiq.environment.environment.validation.network.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@ExtendWith(MockitoExtension.class)
public class AzureExistingPrivateDnsZonesServiceTest {

    private AzureExistingPrivateDnsZonesService underTest = new AzureExistingPrivateDnsZonesService();

    @Test
    void testGetExistingZonesWhenPostgresPresent() {
        NetworkDto networkDto = getNetworkDto("postgresPrivateDnsZoneId");

        Map<AzurePrivateDnsZoneServiceEnum, String> existingZones = underTest.getExistingZones(networkDto);

        assertEquals("postgresPrivateDnsZoneId", existingZones.get(AzurePrivateDnsZoneServiceEnum.POSTGRES));
    }

    @Test
    void testGetExistingZonesWhenPostgresNotPresent() {
        NetworkDto networkDto = getNetworkDto(null);

        Map<AzurePrivateDnsZoneServiceEnum, String> existingZones = underTest.getExistingZones(networkDto);

        assertThat(existingZones).isEmpty();
    }

    @Test
    void testGetServicesWithExistingZonesWhenPostgresWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto("postgresPrivateDnsZoneId");

        Set<AzurePrivateDnsZoneServiceEnum> servicesWithPrivateDnsZones = underTest.getServicesWithExistingZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).hasSize(1);
        assertThat(servicesWithPrivateDnsZones).contains(AzurePrivateDnsZoneServiceEnum.POSTGRES);
    }

    @Test
    void testGetServicesWithExistingZonesWhenNoServicesWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(null);

        Set<AzurePrivateDnsZoneServiceEnum> servicesWithPrivateDnsZones = underTest.getServicesWithExistingZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).isEmpty();
    }

    @Test
    void testGetServiceNamesWithExistingZonesWhenPostgresWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto("postgresPrivateDnsZoneId");

        Set<String> servicesWithPrivateDnsZones = underTest.getServiceNamesWithExistingZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).hasSize(1);
        assertThat(servicesWithPrivateDnsZones).contains(AzurePrivateDnsZoneServiceEnum.POSTGRES.name());
    }

    @Test
    void testgetServiceNamesWithExistingZonesWhenNoServicesWithExistingPrivateDnsZone() {
        NetworkDto networkDto = getNetworkDto(null);

        Set<String> servicesWithPrivateDnsZones = underTest.getServiceNamesWithExistingZones(networkDto);

        assertThat(servicesWithPrivateDnsZones).isEmpty();
    }

    private NetworkDto getNetworkDto(String postgresPrivateDnsZoneId) {
        return NetworkDto.builder()
                .withAzure(
                        AzureParams.builder()
                                .withPrivateDnsZoneId(postgresPrivateDnsZoneId)
                                .build()
                )
                .build();
    }

}
