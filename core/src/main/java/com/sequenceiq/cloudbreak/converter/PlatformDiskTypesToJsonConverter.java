package com.sequenceiq.cloudbreak.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil;

@Component
public class PlatformDiskTypesToJsonConverter extends AbstractConversionServiceAwareConverter<PlatformDisks, PlatformDisksJson> {

    @Override
    public PlatformDisksJson convert(PlatformDisks source) {
        PlatformDisksJson json = new PlatformDisksJson();
        json.setDefaultDisks(PlatformConverterUtil.convertDefaults(source.getDefaultDisks()));
        json.setDiskTypes(PlatformConverterUtil.convertPlatformMap(source.getDiskTypes()));
        json.setDiskMappings(diskMappingsConvert(source.getDiskMappings()));
        return json;
    }

    private Map<String, Map<String, String>> diskMappingsConvert(Map<Platform, Map<String, VolumeParameterType>> diskMappings) {
        Map<String, Map<String, String>> map = new HashMap<>();
        for (Map.Entry<Platform, Map<String, VolumeParameterType>> platformMapEntry : diskMappings.entrySet()) {
            Map<String, String> map1 = new HashMap<>();
            for (Map.Entry<String, VolumeParameterType> stringVolumeParameterTypeEntry : platformMapEntry.getValue().entrySet()) {
                map1.put(stringVolumeParameterTypeEntry.getKey(), stringVolumeParameterTypeEntry.getValue().name());
            }
            map.put(platformMapEntry.getKey().value(), map1);
        }
        return map;
    }

}
