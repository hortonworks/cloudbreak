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

import com.sequenceiq.cloudbreak.cloud.azure.AzureManagedPrivateDnsZoneService;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRegisteredPrivateDnsZoneService;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class AzureExistingPrivateDnsZonesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExistingPrivateDnsZonesService.class);

    Map<AzureManagedPrivateDnsZoneService, String> getExistingManagedZones(NetworkDto networkDto) {
        Map<AzureManagedPrivateDnsZoneService, String> result = new HashMap<>();
        AzureManagedPrivateDnsZoneService serviceEnum = getPrivateDnsZoneService(networkDto);
        Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getDatabasePrivateDnsZoneId)
                .filter(Predicate.not(StringUtils::isEmpty))
                .ifPresent(privateDnsZone -> result.put(serviceEnum, privateDnsZone));
        LOGGER.debug("Existing managed private DNS zones: {}", result);
        return result;
    }

    private AzureManagedPrivateDnsZoneService getPrivateDnsZoneService(NetworkDto networkDto) {
        boolean hasFlexibleServerSubnets = Optional.ofNullable(networkDto.getAzure())
                .map(AzureParams::getFlexibleServerSubnetIds)
                .filter(CollectionUtils::isNotEmpty)
                .isPresent();

        return hasFlexibleServerSubnets ?
                AzureManagedPrivateDnsZoneService.POSTGRES_FLEXIBLE :
                AzureManagedPrivateDnsZoneService.POSTGRES;
    }

    public Map<AzurePrivateDnsZoneDescriptor, String> getExistingManagedZonesAsDescriptors(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneDescriptor, String> result = new HashMap<>();
        AzureManagedPrivateDnsZoneService serviceEnum = getPrivateDnsZoneService(networkDto);
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
                .ifPresent(aksDnsZone -> result.put(AzureRegisteredPrivateDnsZoneService.AKS, aksDnsZone));
        LOGGER.debug("Existing registered only private DNS zones: {}", result);
        return result;
    }

    public Set<AzureManagedPrivateDnsZoneService> getServicesWithExistingManagedZones(NetworkDto networkDto) {
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