package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.AmbariStackDetails
import com.sequenceiq.cloudbreak.api.model.AmbariStackDetailsJson

@Component
class JsonToAmbariStackDetailsConverter : AbstractConversionServiceAwareConverter<AmbariStackDetailsJson, AmbariStackDetails>() {
    override fun convert(source: AmbariStackDetailsJson): AmbariStackDetails {
        val stackDetails = AmbariStackDetails()
        stackDetails.stack = source.stack
        stackDetails.version = source.version
        stackDetails.os = source.os
        stackDetails.utilsRepoId = source.utilsRepoId
        stackDetails.utilsBaseURL = source.utilsBaseURL
        stackDetails.stackRepoId = source.stackRepoId
        stackDetails.stackBaseURL = source.stackBaseURL
        stackDetails.isVerify = source.verify!!
        return stackDetails
    }
}
