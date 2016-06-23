package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.model.PlatformVariants
import com.sequenceiq.cloudbreak.api.model.PlatformVariantsJson
import com.sequenceiq.cloudbreak.converter.util.PlatformConverterUtil

@Component
class PlatformVariantsToJsonConverter : AbstractConversionServiceAwareConverter<PlatformVariants, PlatformVariantsJson>() {

    override fun convert(source: PlatformVariants): PlatformVariantsJson {
        val json = PlatformVariantsJson()
        json.platformToVariants = PlatformConverterUtil.convertPlatformMap(source.platformToVariants)
        json.defaultVariants = PlatformConverterUtil.convertDefaults(source.defaultVariants)
        return json
    }
}
