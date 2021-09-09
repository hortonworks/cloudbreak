package com.sequenceiq.cloudbreak.converter.v4.connectors;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.connector.responses.PlatformDisksV4Response;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;

@Component
public class PlatformDisksToPlatformDisksV4ResponseConverter {

    public PlatformDisksV4Response convert(PlatformDisks source) {
        PlatformDisksV4Response json = new PlatformDisksV4Response();
        json.setDefaultDisks(PlatformConverterUtil.convertDefaults(source.getDefaultDisks()));
        json.setDiskTypes(PlatformConverterUtil.convertPlatformMap(source.getDiskTypes()));
        json.setDiskMappings(diskMappingsConvert(source.getDiskMappings()));
        json.setDisplayNames(PlatformConverterUtil.convertDisplayNameMap(source.getDiskDisplayNames()));
        return json;
    }

    private Map<String, Map<String, String>> diskMappingsConvert(Map<Platform, Map<String, VolumeParameterType>> diskMappings) {
        Map<String, Map<String, String>> map = new HashMap<>();
        for (Entry<Platform, Map<String, VolumeParameterType>> platformMapEntry : diskMappings.entrySet()) {
            Map<String, String> map1 = new HashMap<>();
            for (Entry<String, VolumeParameterType> stringVolumeParameterTypeEntry : platformMapEntry.getValue().entrySet()) {
                map1.put(stringVolumeParameterTypeEntry.getKey(), stringVolumeParameterTypeEntry.getValue().name());
            }
            map.put(platformMapEntry.getKey().value(), map1);
        }
        return map;
    }

}
