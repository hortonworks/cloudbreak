package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.PlatformOrchestratorsJson
import com.sequenceiq.cloudbreak.cloud.model.PlatformOrchestrators
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil

@Component
class PlatformOrchestratorsToJsonConverter : AbstractConversionServiceAwareConverter<PlatformOrchestrators, PlatformOrchestratorsJson>() {

    override fun convert(source: PlatformOrchestrators): PlatformOrchestratorsJson {
        val json = PlatformOrchestratorsJson()
        json.orchestrators = PlatformConverterUtil.convertPlatformMap(source.orchestrators)
        json.defaults = PlatformConverterUtil.convertDefaults(source.defaults)

        return json
    }
}
