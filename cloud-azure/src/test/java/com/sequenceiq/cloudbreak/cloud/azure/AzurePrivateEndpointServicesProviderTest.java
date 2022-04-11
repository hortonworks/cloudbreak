package com.sequenceiq.cloudbreak.cloud.azure;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

@ExtendWith(MockitoExtension.class)
public class AzurePrivateEndpointServicesProviderTest {

    private final AzurePrivateEndpointServicesProvider underTest = new AzurePrivateEndpointServicesProvider();

    @Test
    void testGetCdpManagedDnsZonesWhenExistingDnsZone() {
        setEnabledPrivateEndpointServices(List.of("postgresqlServer"));
        Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingPrivateDnsZone = Set.of(AzurePrivateDnsZoneServiceEnum.POSTGRES);

        List<AzurePrivateDnsZoneServiceEnum> privateEndpointServices = underTest.getCdpManagedDnsZones(servicesWithExistingPrivateDnsZone);

        assertThat(privateEndpointServices).isEmpty();
    }

    @Test
    void testGetCdpManagedDnsZonesWhenCdpManagesZone() {
        setEnabledPrivateEndpointServices(List.of("postgresqlServer"));
        Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingPrivateDnsZone = Set.of();

        List<AzurePrivateDnsZoneServiceEnum> privateEndpointServices = underTest.getCdpManagedDnsZones(servicesWithExistingPrivateDnsZone);

        assertThat(privateEndpointServices).hasSize(1);
        assertThat(privateEndpointServices).contains(AzurePrivateDnsZoneServiceEnum.POSTGRES);
    }

    @Test
    void testGetCdpManagedDnsZonesWhenServiceNotEnabled() {
        setEnabledPrivateEndpointServices(List.of());
        Set<AzurePrivateDnsZoneServiceEnum> servicesWithExistingPrivateDnsZone = Set.of();

        List<AzurePrivateDnsZoneServiceEnum> privateEndpointServices = underTest.getCdpManagedDnsZones(servicesWithExistingPrivateDnsZone);

        assertThat(privateEndpointServices).isEmpty();
    }

    private void setEnabledPrivateEndpointServices(List<String> enabledPrivateEndpointServices) {
        Field privateEndpointServicesField = ReflectionUtils.findField(AzurePrivateEndpointServicesProvider.class, "privateEndpointServices");
        ReflectionUtils.makeAccessible(privateEndpointServicesField);
        ReflectionUtils.setField(privateEndpointServicesField, underTest, enabledPrivateEndpointServices);
    }

}
