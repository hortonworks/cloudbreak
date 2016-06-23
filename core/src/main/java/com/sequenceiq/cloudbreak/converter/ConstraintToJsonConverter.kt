package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.api.model.ConstraintJson

@Component
class ConstraintToJsonConverter : AbstractConversionServiceAwareConverter<Constraint, ConstraintJson>() {

    override fun convert(source: Constraint): ConstraintJson {
        val constraintJson = ConstraintJson()
        if (source.constraintTemplate != null) {
            constraintJson.constraintTemplateName = source.constraintTemplate.name
        }
        if (source.instanceGroup != null) {
            constraintJson.instanceGroupName = source.instanceGroup.groupName
        }
        constraintJson.hostCount = source.hostCount
        return constraintJson
    }
}
