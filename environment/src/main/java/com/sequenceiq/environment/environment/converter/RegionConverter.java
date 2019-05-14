package com.sequenceiq.environment.environment.converter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.environment.model.response.CompactRegionV1Response;
import com.sequenceiq.environment.environment.domain.Region;

@Component
public class RegionConverter {
    public CompactRegionV1Response convertRegions(Set<Region> regions) {
        CompactRegionV1Response compactRegionResponse = new CompactRegionV1Response();
        Set<String> values = new HashSet<>();
        Map<String, String> displayNames = new HashMap<>();
        for (Region region : regions) {
            values.add(region.getName());
            displayNames.put(region.getName(), region.getDisplayName());
        }
        compactRegionResponse.setRegions(values);
        compactRegionResponse.setDisplayNames(displayNames);
        return compactRegionResponse;
    }
}
