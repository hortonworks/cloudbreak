package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.common.type.ResourceStatus
import com.sequenceiq.cloudbreak.domain.ConstraintTemplate
import com.sequenceiq.cloudbreak.api.model.ConstraintTemplateRequest

@Component
class JsonToConstraintTemplateConverter : AbstractConversionServiceAwareConverter<ConstraintTemplateRequest, ConstraintTemplate>() {
    override fun convert(source: ConstraintTemplateRequest): ConstraintTemplate {
        val constraintTemplate = ConstraintTemplate()
        constraintTemplate.cpu = source.cpu
        constraintTemplate.memory = source.memory
        constraintTemplate.disk = source.disk
        constraintTemplate.orchestratorType = source.orchestratorType
        constraintTemplate.name = source.name
        constraintTemplate.description = source.description
        constraintTemplate.status = ResourceStatus.USER_MANAGED
        return constraintTemplate
    }

}
