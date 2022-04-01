package com.sequenceiq.environment.environment.validation.network.azure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzurePrivateDnsZoneServiceEnum;
import com.sequenceiq.environment.network.dto.NetworkDto;

@Service
public class AzureExistingPrivateDnsZonesService {

    public Map<AzurePrivateDnsZoneServiceEnum, String> getExistingZones(NetworkDto networkDto) {
        Map<AzurePrivateDnsZoneServiceEnum, String> result = new HashMap<>();
        Optional.ofNullable(networkDto.getAzure().getPrivateDnsZoneId())
                .ifPresent(privateDnsZone -> result.put(AzurePrivateDnsZoneServiceEnum.POSTGRES, privateDnsZone));
        return result;
    }

    public Set<AzurePrivateDnsZoneServiceEnum> getServicesWithExistingZones(NetworkDto networkDto) {
        return getExistingZones(networkDto).keySet();
    }

    public boolean hasNoExistingZones(NetworkDto networkDto) {
        return getExistingZones(networkDto).isEmpty();
    }

    public Set<String> getServiceNamesWithExistingZones(NetworkDto networkDto) {
        return getServicesWithExistingZones(networkDto).stream()
                .map(Enum::name)
                .collect(Collectors.toSet());
    }

}
