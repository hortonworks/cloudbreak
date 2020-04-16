package com.sequenceiq.environment.environment.v1.converter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.response.CompactRegionResponse;
import com.sequenceiq.environment.environment.domain.Region;

@Component
public class RegionConverter {
    public CompactRegionResponse convertRegions(Set<Region> regions) {
        CompactRegionResponse compactRegionResponse = new CompactRegionResponse();
        List<String> values = new ArrayList<>();
        Map<String, String> displayNames = new HashMap<>();
        for (Region region : regions) {
            values.add(region.getName());
            displayNames.put(region.getName(), region.getDisplayName());
        }
        compactRegionResponse.setNames(values);
        compactRegionResponse.setDisplayNames(displayNames);
        return compactRegionResponse;
    }
}
