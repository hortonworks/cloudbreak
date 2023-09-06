package com.sequenceiq.environment.environment.validation.network.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneRegistrationEnum;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.environment.network.dto.AzureParams;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class AzureExistingPrivateDnsZonesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExistingPrivateDnsZonesService.class);

    public Map<AzurePrivateDnsZoneServiceEnum, String> getExistingManagedZones(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneServiceEnum, String> result = new HashMap<>();
        Optional.ofNullable(networkDto.getAzure() != null ? Strings.emptyToNull(networkDto.getAzure().getDatabasePrivateDnsZoneId()) : null)
                .ifPresent(privateDnsZone -> result.put(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZone));
        LOGGER.debug("Existing managed private DNS zones: {}", result);
        return result;
    }

    public Map<AzurePrivateDnsZoneDescriptor, String> getExistingManagedZonesAsDescriptors(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneDescriptor, String> result = new HashMap<>();
        boolean hasFlexibleServerSubnets = CollectionUtils.isNotEmpty(networkDto.getAzure().getFlexibleServerSubnetIds());
        AzurePrivateDnsZoneServiceEnum serviceEnum = hasFlexibleServerSubnets ?
                AzurePrivateDnsZoneServiceEnum.POSTGRES_FLEXIBLE :
                AzurePrivateDnsZoneServiceEnum.POSTGRES;
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
                .ifPresent(aksDnsZone -> result.put(AzurePrivateDnsZoneRegistrationEnum.AKS, aksDnsZone));
        LOGGER.debug("Existing registered only private DNS zones: {}", result);
        return result;
    }

    public Set<AzurePrivateDnsZoneServiceEnum> getServicesWithExistingManagedZones(NetworkDto networkDto) {
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
