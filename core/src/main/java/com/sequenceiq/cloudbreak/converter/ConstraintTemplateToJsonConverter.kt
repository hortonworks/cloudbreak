package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.ConstraintTemplate
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateResponse

@Component
class ConstraintTemplateToJsonConverter : AbstractConversionServiceAwareConverter<ConstraintTemplate, ConstraintTemplateResponse>() {
    override fun convert(source: ConstraintTemplate): ConstraintTemplateResponse {
        val constraintTemplateResponse = ConstraintTemplateResponse()
        constraintTemplateResponse.id = source.id
        constraintTemplateResponse.name = source.name
        constraintTemplateResponse.isPublicInAccount = source.isPublicInAccount
        constraintTemplateResponse.description = if (source.description == null) "" else source.description
        constraintTemplateResponse.cpu = source.cpu
        constraintTemplateResponse.memory = source.memory
        constraintTemplateResponse.disk = source.disk
        constraintTemplateResponse.orchestratorType = source.orchestratorType

        return constraintTemplateResponse
    }
}
