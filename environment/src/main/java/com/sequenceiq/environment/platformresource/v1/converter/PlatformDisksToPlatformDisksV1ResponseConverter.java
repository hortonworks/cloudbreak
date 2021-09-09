package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.cloud.model.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDisksResponse;
import com.sequenceiq.environment.platformresource.v1.PlatformConverterUtil;

@Component
public class PlatformDisksToPlatformDisksV1ResponseConverter {

    public PlatformDisksResponse convert(PlatformDisks source) {
        PlatformDisksResponse json = new PlatformDisksResponse();
        json.setDefaultDisks(convertDefaults(source.getDefaultDisks()));
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

    public Map<String, String> convertDefaults(Map<Platform, DiskType> vms) {
        Map<String, String> result = Maps.newHashMap();
        for (Entry<Platform, DiskType> entry : vms.entrySet()) {
            result.put(entry.getKey().value(), entry.getValue().value());
        }
        return result;
    }

}
