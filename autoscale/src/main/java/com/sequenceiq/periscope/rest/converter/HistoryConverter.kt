package com.sequenceiq.periscope.rest.converter

import org.springframework.stereotype.Component

import com.sequenceiq.periscope.domain.History
import com.sequenceiq.periscope.api.model.HistoryJson

@Component
class HistoryConverter : AbstractConverter<HistoryJson, History>() {

    override fun convert(source: History): HistoryJson {
        val json = HistoryJson()
        json.id = source.id
        json.adjustment = source.adjustment
        json.adjustmentType = source.adjustmentType
        json.alertType = source.alertType
        json.cbStackId = source.cbStackId
        json.clusterId = source.clusterId
        json.hostGroup = source.hostGroup
        json.originalNodeCount = source.originalNodeCount
        json.properties = source.properties
        json.scalingStatus = source.scalingStatus
        json.timestamp = source.timestamp
        json.statusReason = source.statusReason
        return json
    }
}
