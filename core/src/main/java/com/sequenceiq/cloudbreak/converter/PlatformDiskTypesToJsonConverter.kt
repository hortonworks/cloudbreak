package com.sequenceiq.cloudbreak.converter

import java.util.HashMap

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformDisks
import com.sequenceiq.cloudbreak.api.model.PlatformDisksJson
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil

@Component
class PlatformDiskTypesToJsonConverter : AbstractConversionServiceAwareConverter<PlatformDisks, PlatformDisksJson>() {

    override fun convert(source: PlatformDisks): PlatformDisksJson {
        val json = PlatformDisksJson()
        json.defaultDisks = PlatformConverterUtil.convertDefaults(source.defaultDisks)
        json.diskTypes = PlatformConverterUtil.convertPlatformMap(source.diskTypes)
        json.diskMappings = diskMappingsConvert(source.diskMappings)
        return json
    }

    private fun diskMappingsConvert(diskMappings: Map<Platform, Map<String, VolumeParameterType>>): Map<String, Map<String, String>> {
        val map = HashMap<String, Map<String, String>>()
        for (platformMapEntry in diskMappings.entries) {
            val map1 = HashMap<String, String>()
            for (stringVolumeParameterTypeEntry in platformMapEntry.value.entries) {
                map1.put(stringVolumeParameterTypeEntry.key, stringVolumeParameterTypeEntry.value.name)
            }
            map.put(platformMapEntry.key.value(), map1)
        }
        return map
    }

}
