package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.api.model.FailurePolicyJson
import com.sequenceiq.cloudbreak.domain.FailurePolicy

@Component
class FailurePolicyToJsonConverter : AbstractConversionServiceAwareConverter<FailurePolicy, FailurePolicyJson>() {
    override fun convert(entity: FailurePolicy): FailurePolicyJson {
        val json = FailurePolicyJson()
        json.adjustmentType = entity.adjustmentType
        json.id = entity.id
        json.threshold = if (entity.threshold == null) 0 else entity.threshold
        return json
    }
}
