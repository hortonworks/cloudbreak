package com.sequenceiq.cloudbreak.converter

import java.util.Date

import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.api.model.CloudbreakEventsJson

@Component
class JsonToCloudbreakEventConverter : AbstractConversionServiceAwareConverter<CloudbreakEventsJson, CloudbreakEvent>() {

    override fun convert(json: CloudbreakEventsJson): CloudbreakEvent {
        val entity = CloudbreakEvent()
        entity.account = json.account
        entity.blueprintId = json.blueprintId
        entity.blueprintName = json.blueprintName
        entity.cloud = json.cloud
        entity.eventMessage = json.eventMessage
        entity.eventType = json.eventType
        entity.eventTimestamp = Date(json.eventTimestamp)
        entity.region = json.region
        entity.availabilityZone = json.availabilityZone
        entity.owner = json.owner
        entity.stackId = json.stackId
        entity.stackName = json.stackName
        entity.stackStatus = json.stackStatus
        entity.nodeCount = json.nodeCount
        entity.instanceGroup = json.instanceGroup
        entity.clusterStatus = json.clusterStatus
        entity.clusterId = json.clusterId
        entity.clusterName = json.clusterName
        return entity
    }
}
