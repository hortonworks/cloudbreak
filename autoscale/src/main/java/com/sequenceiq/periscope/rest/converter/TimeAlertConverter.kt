package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.api.model.TimeAlertJson
import com.sequenceiq.periscope.domain.TimeAlert

@Component
class TimeAlertConverter : AbstractConverter<TimeAlertJson, TimeAlert>() {

    override fun convert(source: TimeAlertJson): TimeAlert {
        val alarm = TimeAlert()
        alarm.name = source.alertName
        alarm.description = source.description
        alarm.cron = source.cron
        alarm.timeZone = source.timeZone
        return alarm
    }

    override fun convert(source: TimeAlert): TimeAlertJson {
        val json = TimeAlertJson()
        json.id = source.id
        json.alertName = source.name
        json.cron = source.cron
        json.timeZone = source.timeZone
        json.description = source.description
        json.scalingPolicyId = source.scalingPolicyId
        return json
    }

}
