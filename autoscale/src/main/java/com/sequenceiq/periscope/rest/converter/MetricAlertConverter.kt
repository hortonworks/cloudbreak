package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.model.MetricAlertJson
import com.sequenceiq.periscope.domain.MetricAlert

@Component
class MetricAlertConverter : AbstractConverter<MetricAlertJson, MetricAlert>() {

    override fun convert(source: MetricAlertJson): MetricAlert {
        val alert = MetricAlert()
        alert.name = source.alertName
        alert.description = source.description
        alert.definitionName = source.alertDefinition
        alert.period = source.period
        alert.alertState = source.alertState
        return alert
    }

    override fun convert(source: MetricAlert): MetricAlertJson {
        val json = MetricAlertJson()
        json.id = source.id
        json.scalingPolicyId = source.scalingPolicyId
        json.alertName = source.name
        json.description = source.description
        json.period = source.period
        json.alertDefinition = source.definitionName
        json.alertState = source.alertState
        return json
    }

}
