package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson

@Component
class AmbariStackDetailsToJsonConverter : AbstractConversionServiceAwareConverter<AmbariStackDetails, AmbariStackDetailsJson>() {
    override fun convert(source: AmbariStackDetails): AmbariStackDetailsJson {
        val json = AmbariStackDetailsJson()
        json.stack = source.stack
        json.version = source.version
        json.os = source.os
        json.utilsRepoId = source.utilsRepoId
        json.utilsBaseURL = source.utilsBaseURL
        json.stackRepoId = source.stackRepoId
        json.stackBaseURL = source.stackBaseURL
        json.verify = source.isVerify
        return json
    }
}
