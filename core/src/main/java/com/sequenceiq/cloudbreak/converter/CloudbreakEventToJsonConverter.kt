package com.sequenceiq.cloudbreak.converter

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

@Component
class CloudbreakEventToJsonConverter : AbstractConversionServiceAwareConverter<CloudbreakEvent, CloudbreakEventsJson>() {

    override fun convert(entity: CloudbreakEvent): CloudbreakEventsJson {
        val json = CloudbreakEventsJson()
        json.account = entity.account
        json.blueprintId = entity.blueprintId
        json.blueprintName = entity.blueprintName
        json.cloud = entity.cloud
        json.eventMessage = entity.eventMessage
        json.eventType = entity.eventType
        json.eventTimestamp = entity.eventTimestamp.time
        json.region = entity.region
        json.owner = entity.owner
        json.stackId = entity.stackId
        json.stackName = entity.stackName
        json.stackStatus = entity.stackStatus
        json.nodeCount = entity.nodeCount
        json.instanceGroup = entity.instanceGroup
        json.clusterStatus = entity.clusterStatus
        json.clusterId = entity.clusterId
        json.clusterName = entity.clusterName
        return json
    }
}
