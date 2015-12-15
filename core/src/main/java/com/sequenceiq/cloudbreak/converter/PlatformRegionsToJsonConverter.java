package com.sequenceiq.cloudbreak.converter;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.api.client.util.Maps;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.model.PlatformRegionsJson;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;

@Component
public class PlatformRegionsToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformRegions, PlatformRegionsJson> {

    @Override
    public PlatformRegionsJson convert(PlatformRegions source) {
        PlatformRegionsJson json = new PlatformRegionsJson();
        json.setAvailabilityZones(convertAvailibilityZones(source.getAvailabiltyZones()));
        json.setDefaultRegions(PlatformConverterUtil.convertDefaults(source.getDefaultRegions()));
        json.setRegions(PlatformConverterUtil.convertPlatformMap(source.getRegions()));
        return json;
    }

    private Map<String, Map<String, Collection<String>>> convertAvailibilityZones(Map<Platform, Map<Region, List<AvailabilityZone>>> availabilityZones) {
        Map<String, Map<String, Collection<String>>> result = Maps.newHashMap();
        for (Map.Entry<Platform, Map<Region, List<AvailabilityZone>>> entry : availabilityZones.entrySet()) {
            result.put(entry.getKey().value(), PlatformConverterUtil.convertPlatformMap(entry.getValue()));
        }
        return result;

    }
}
