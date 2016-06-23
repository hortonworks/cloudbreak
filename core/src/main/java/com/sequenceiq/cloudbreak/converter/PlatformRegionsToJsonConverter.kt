package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.google.api.client.util.Maps
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone
import com.sequenceiq.cloudbreak.cloud.model.Platform
import com.sequenceiq.cloudbreak.cloud.model.PlatformRegions
import com.sequenceiq.cloudbreak.cloud.model.Region
import com.sequenceiq.cloudbreak.api.model.PlatformRegionsJson
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil

@Component
class PlatformRegionsToJsonConverter : AbstractConversionServiceAwareConverter<PlatformRegions, PlatformRegionsJson>() {

    override fun convert(source: PlatformRegions): PlatformRegionsJson {
        val json = PlatformRegionsJson()
        json.availabilityZones = convertAvailibilityZones(source.availabiltyZones)
        json.defaultRegions = PlatformConverterUtil.convertDefaults(source.defaultRegions)
        json.regions = PlatformConverterUtil.convertPlatformMap(source.regions)
        return json
    }

    private fun convertAvailibilityZones(availabilityZones: Map<Platform, Map<Region, List<AvailabilityZone>>>): Map<String, Map<String, Collection<String>>> {
        val result = Maps.newHashMap<String, Map<String, Collection<String>>>()
        for (entry in availabilityZones.entries) {
            result.put(entry.key.value(), PlatformConverterUtil.convertPlatformMap(entry.value))
        }
        return result

    }
}
