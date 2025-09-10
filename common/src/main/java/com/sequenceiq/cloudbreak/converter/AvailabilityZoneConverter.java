package com.sequenceiq.cloudbreak.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.network.NetworkConstants;

@Component
public class AvailabilityZoneConverter {
    public Set<String> getAvailabilityZonesFromJsonAttributes(Json attributes) {
        Set<String> zoneList = new HashSet<>();
        if (attributes != null) {
            zoneList.addAll((List<String>) attributes
                    .getMap()
                    .getOrDefault(NetworkConstants.AVAILABILITY_ZONES, new ArrayList<>()));
        }
        return zoneList;
    }

    public Json getJsonAttributesWithAvailabilityZones(Set<String> zones, Json existingAttributes) {
        Json newAttributes = existingAttributes;
        if (zones != null && !zones.isEmpty()) {
            Map<String, Object> existingAttributesMap = (existingAttributes != null) ? existingAttributes.getMap() : new HashMap<>();
            existingAttributesMap.put(NetworkConstants.AVAILABILITY_ZONES, zones);
            newAttributes = new Json(existingAttributesMap);
        }
        return newAttributes;
    }
}
