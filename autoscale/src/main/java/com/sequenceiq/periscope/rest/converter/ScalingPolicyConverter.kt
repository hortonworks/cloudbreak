package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.model.ScalingPolicyJson
import com.sequenceiq.periscope.domain.BaseAlert
import com.sequenceiq.periscope.domain.ScalingPolicy

@Component
class ScalingPolicyConverter : AbstractConverter<ScalingPolicyJson, ScalingPolicy>() {

    override fun convert(source: ScalingPolicyJson): ScalingPolicy {
        val policy = ScalingPolicy()
        policy.adjustmentType = source.adjustmentType
        policy.name = source.name
        policy.scalingAdjustment = source.scalingAdjustment
        policy.hostGroup = source.hostGroup
        return policy
    }

    override fun convert(source: ScalingPolicy): ScalingPolicyJson {
        val json = ScalingPolicyJson()
        json.id = source.id
        json.adjustmentType = source.adjustmentType
        val alert = source.alert
        json.alertId = alert?.id
        json.name = source.name
        json.scalingAdjustment = source.scalingAdjustment
        json.hostGroup = source.hostGroup
        return json
    }
}
