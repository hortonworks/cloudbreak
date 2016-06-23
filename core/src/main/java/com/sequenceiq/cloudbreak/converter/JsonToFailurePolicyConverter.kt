package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.domain.FailurePolicy

@Component
class JsonToFailurePolicyConverter : AbstractConversionServiceAwareConverter<FailurePolicyJson, FailurePolicy>() {
    override fun convert(json: FailurePolicyJson): FailurePolicy {
        val stackFailurePolicy = FailurePolicy()
        stackFailurePolicy.adjustmentType = json.adjustmentType
        stackFailurePolicy.id = json.id
        stackFailurePolicy.threshold = if (json.threshold == null) 0 else json.threshold
        return stackFailurePolicy
    }
}
