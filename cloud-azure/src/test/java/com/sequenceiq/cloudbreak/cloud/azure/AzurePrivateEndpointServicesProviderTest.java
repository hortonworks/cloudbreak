package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.EnumSource.Mode;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;

@ExtendWith(MockitoExtension.class)
public class AzurePrivateEndpointServicesProviderTest {

    private final AzurePrivateEndpointServicesProvider underTest = new AzurePrivateEndpointServicesProvider();

    @Test
    void testGetCdpManagedDnsZonesWhenExistingDnsZone() {
        setEnabledPrivateEndpointServices(List.of("postgresqlServer"));
        Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone = Set.of(AzureManagedPrivateDnsZoneService.POSTGRES);

        List<AzureManagedPrivateDnsZoneService> privateEndpointServices = underTest.getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone,
                PrivateDatabaseVariant.POSTGRES_WITH_EXISTING_DNS_ZONE);

        assertThat(privateEndpointServices).isEmpty();
    }

    @Test
    void testGetCdpManagedDnsZonesWhenCdpManagesZone() {
        setEnabledPrivateEndpointServices(List.of("postgresqlServer"));
        Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone = Set.of();

        List<AzureManagedPrivateDnsZoneService> privateEndpointServices = underTest.getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone,
                PrivateDatabaseVariant.POSTGRES_WITH_NEW_DNS_ZONE);

        assertThat(privateEndpointServices).hasSize(1);
        assertThat(privateEndpointServices).contains(AzureManagedPrivateDnsZoneService.POSTGRES);
    }

    @ParameterizedTest
    @EnumSource(value = PrivateDatabaseVariant.class, mode = Mode.INCLUDE, names = {"POSTGRES_WITH_NEW_DNS_ZONE", "FLEXIBLE_POSTGRES_WITH_NEW_DNS_ZONE"})
    void testGetCdpManagedDnsZonesWhenBothDBServicesEnabledAndCdpManagesZone(PrivateDatabaseVariant variant) {
        setEnabledPrivateEndpointServices(List.of("postgresqlServer", "flexiblePostgresqlServer"));
        Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone = Set.of();

        List<AzureManagedPrivateDnsZoneService> privateEndpointServices = underTest.getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone, variant);

        assertThat(privateEndpointServices).hasSize(1);
        assertThat(privateEndpointServices).contains(variant == PrivateDatabaseVariant.POSTGRES_WITH_NEW_DNS_ZONE ?
                AzureManagedPrivateDnsZoneService.POSTGRES :
                AzureManagedPrivateDnsZoneService.POSTGRES_FLEXIBLE);
    }

    @Test
    void testGetCdpManagedDnsZonesWhenServiceNotEnabled() {
        setEnabledPrivateEndpointServices(List.of());
        Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone = Set.of();

        List<AzureManagedPrivateDnsZoneService> privateEndpointServices = underTest.getCdpManagedDnsZoneServices(servicesWithExistingPrivateDnsZone,
                PrivateDatabaseVariant.POSTGRES_WITH_NEW_DNS_ZONE);

        assertThat(privateEndpointServices).isEmpty();
    }

    private void setEnabledPrivateEndpointServices(List<String> enabledPrivateEndpointServices) {
        Field privateEndpointServicesField = ReflectionUtils.findField(AzurePrivateEndpointServicesProvider.class, "privateEndpointServices");
        ReflectionUtils.makeAccessible(privateEndpointServicesField);
        ReflectionUtils.setField(privateEndpointServicesField, underTest, enabledPrivateEndpointServices);
    }

}
