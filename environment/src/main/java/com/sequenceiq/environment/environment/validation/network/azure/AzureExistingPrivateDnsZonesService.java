package com.sequenceiq.environment.environment.validation.network.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneServiceType;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRegisteredPrivateDnsZoneServiceType;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class AzureExistingPrivateDnsZonesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExistingPrivateDnsZonesService.class);

    Map<AzureManagedPrivateDnsZoneServiceType, String> getExistingManagedZones(NetworkDto networkDto) {
        Map<AzureManagedPrivateDnsZoneServiceType, String> result = new HashMap<>();
        AzureManagedPrivateDnsZoneServiceType serviceEnum = getPrivateDnsZoneService(networkDto);
        Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getDatabasePrivateDnsZoneId)
                .filter(Predicate.not(StringUtils::isEmpty))
                .ifPresent(privateDnsZone -> result.put(serviceEnum, privateDnsZone));
        LOGGER.debug("Existing managed private DNS zones: {}", result);
        return result;
    }

    private AzureManagedPrivateDnsZoneServiceType getPrivateDnsZoneService(NetworkDto networkDto) {
        // with the introduction of Private Endpoint support for Flexible Server,
        // there is no way to distinguish private Single and Flexible environment setups
        // so Flexible Server is the only supported environment level option as Flexible Server entitlement become default entitlement

        boolean hasFlexibleServerSubnets = Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getFlexibleServerSubnetIds)
                .filter(CollectionUtils::isNotEmpty)
                .isPresent();

        return hasFlexibleServerSubnets ?
                AzureManagedPrivateDnsZoneServiceType.POSTGRES_FLEXIBLE :
                AzureManagedPrivateDnsZoneServiceType.POSTGRES_FLEXIBLE_FOR_PRIVATE_ENDPOINT;
    }

    public Map<AzurePrivateDnsZoneDescriptor, String> getExistingManagedZonesAsDescriptors(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneDescriptor, String> result = new HashMap<>();
        AzureManagedPrivateDnsZoneServiceType serviceEnum = getPrivateDnsZoneService(networkDto);
        Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getDatabasePrivateDnsZoneId)
                .filter(Predicate.not(String::isEmpty))
                .ifPresent(privateDnsZone -> result.put(serviceEnum, privateDnsZone));
        LOGGER.debug("Existing managed private DNS zones: {}", result);
        return result;
    }

    public Map<AzurePrivateDnsZoneDescriptor, String> getExistingRegisteredOnlyZonesAsDescriptors(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneDescriptor, String> result = new HashMap<>();
        Optional.ofNullable(networkDto.getAzure().getAksPrivateDnsZoneId())
                .ifPresent(aksDnsZone -> result.put(AzureRegisteredPrivateDnsZoneServiceType.AKS, aksDnsZone));
        LOGGER.debug("Existing registered only private DNS zones: {}", result);
        return result;
    }

    public Set<AzureManagedPrivateDnsZoneServiceType> getServicesWithExistingManagedZones(NetworkDto networkDto) {
        return getExistingManagedZones(networkDto).keySet();
    }

    public boolean hasNoExistingManagedZones(NetworkDto networkDto) {
        return getExistingManagedZones(networkDto).isEmpty();
    }

    public boolean hasNoExistingRegisteredOnlyZones(NetworkDto networkDto) {
        return getExistingRegisteredOnlyZonesAsDescriptors(networkDto).isEmpty();
    }

    public Set<String> getServiceNamesWithExistingManagedZones(NetworkDto networkDto) {
        return getServicesWithExistingManagedZones(networkDto).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }
}