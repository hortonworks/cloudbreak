package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Constraint
import com.sequenceiq.cloudbreak.api.model.ConstraintJson

@Component
class JsonToConstraintConverter : AbstractConversionServiceAwareConverter<ConstraintJson, Constraint>() {

    override fun convert(source: ConstraintJson): Constraint {
        val constraint = Constraint()
        constraint.hostCount = source.hostCount
        return constraint
    }
}
