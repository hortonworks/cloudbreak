package com.sequenceiq.environment.environment.validation.network.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneDescriptor;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneRegistrationEnum;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class AzureExistingPrivateDnsZonesService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureExistingPrivateDnsZonesService.class);

    public Map<AzurePrivateDnsZoneServiceEnum, String> getExistingServiceZones(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneServiceEnum, String> result = new HashMap<>();
        Optional.ofNullable(networkDto.getAzure().getDatabasePrivateDnsZoneId())
                .ifPresent(privateDnsZone -> result.put(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZone));
        LOGGER.debug("Existing service private DNS zones: {}", result);
        return result;
    }

    public Map<AzurePrivateDnsZoneDescriptor, String> getExistingServiceZonesAsDescriptors(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneDescriptor, String> result = new HashMap<>();
        Optional.ofNullable(networkDto.getAzure().getDatabasePrivateDnsZoneId())
                .ifPresent(privateDnsZone -> result.put(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZone));
        return result;
    }

    public Map<AzurePrivateDnsZoneDescriptor, String> getExistingRegisteredOnlyZonesAsDescriptors(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneDescriptor, String> result = new HashMap<>();
        Optional.ofNullable(networkDto.getAzure().getAksPrivateDnsZoneId())
                .ifPresent(aksDnsZone -> result.put(AzurePrivateDnsZoneRegistrationEnum.AKS, aksDnsZone));
        LOGGER.debug("Existing registered only private DNS zones: {}", result);
        return result;
    }

    public Set<AzurePrivateDnsZoneServiceEnum> getServicesWithExistingZones(NetworkDto networkDto) {
        return getExistingServiceZones(networkDto).keySet();
    }

    public boolean hasNoExistingServiceZones(NetworkDto networkDto) {
        return getExistingServiceZones(networkDto).isEmpty();
    }

    public boolean hasNoExistingRegisteredOnlyZones(NetworkDto networkDto) {
        return getExistingRegisteredOnlyZonesAsDescriptors(networkDto).isEmpty();
    }

    public Set<String> getServiceNamesWithExistingZones(NetworkDto networkDto) {
        return getServicesWithExistingZones(networkDto).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

}
