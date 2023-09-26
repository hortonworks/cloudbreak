package com.sequenceiq.cloudbreak.cloud.azure;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.network.PrivateDatabaseVariant;
import com.sequenceiq.common.model.AzureDatabaseType;

@Service
public class AzurePrivateEndpointServicesProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzurePrivateEndpointServicesProvider.class);

    @Value("${cb.arm.privateendpoint.services:}")
    private List<String> privateEndpointServices;

    public List<AzureManagedPrivateDnsZoneService> getCdpManagedDnsZoneServices(Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone,
            PrivateDatabaseVariant privateDatabaseVariant) {
        AzureDatabaseType databaseType = privateDatabaseVariant.getDatabaseType();
        List<AzureManagedPrivateDnsZoneService> enabledPrivateEndpointServices =
                getEnabledPrivateEndpointServices(servicesWithExistingPrivateDnsZone, databaseType);
        LOGGER.debug("Services with existing private dns zones: {}, services where new private DNS zone needs to be created: {}",
                servicesWithExistingPrivateDnsZone, enabledPrivateEndpointServices);
        return enabledPrivateEndpointServices;
    }

    private List<AzureManagedPrivateDnsZoneService> getEnabledPrivateEndpointServices(
            Set<AzureManagedPrivateDnsZoneService> servicesWithExistingPrivateDnsZone, AzureDatabaseType databaseType) {
        List<AzureManagedPrivateDnsZoneService> serviceEnumList = privateEndpointServices.stream()
                .map(AzureManagedPrivateDnsZoneService::getBySubResource)
                .filter(Predicate.not(servicesWithExistingPrivateDnsZone::contains))
                .filter(service ->
                        service.getResourceFamily() != AzureResourceFamily.DATABASE ||
                                service.getSubResource().equals(databaseType.shortName()))
                .collect(Collectors.toList());
        LOGGER.debug("Enabled private endpoint services: {}", serviceEnumList);
        return serviceEnumList;
    }

}
