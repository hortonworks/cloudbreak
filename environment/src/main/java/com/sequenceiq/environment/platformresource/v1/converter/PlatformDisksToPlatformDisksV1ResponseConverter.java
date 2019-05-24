package com.sequenceiq.environment.platformresource.v1.converter;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.environment.api.v1.platformresource.model.PlatformDisksResponse;
import com.sequenceiq.environment.platformresource.v1.PlatformConverterUtil;

@Component
public class PlatformDisksToPlatformDisksV1ResponseConverter extends AbstractConversionServiceAwareConverter<PlatformDisks, PlatformDisksResponse> {

    @Override
    public PlatformDisksResponse convert(PlatformDisks source) {
        PlatformDisksResponse json = new PlatformDisksResponse();
        json.setDefaultDisks(source.getDefaultDisks().entrySet().stream().collect(Collectors.toMap(String::valueOf, String::valueOf)));
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
