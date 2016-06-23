package com.sequenceiq.cloudbreak.service.events

class InstanceGroupEventData : CloudbreakEventData {

    var instanceGroupName: String? = null

    constructor(entityId: Long?, eventType: String, eventMessage: String) : super(entityId, eventType, eventMessage) {
    }

    constructor(entityId: Long?, eventType: String, eventMessage: String, instanceGroup: String) : super(entityId, eventType, eventMessage) {
        this.instanceGroupName = instanceGroup
    }

    override fun toString(): String {
        val sb = StringBuilder("CloudbreakEventData{")
        sb.append("entityId=").append(super.entityId)
        sb.append(", eventType='").append(super.eventType).append('\'')
        sb.append(", eventMessage='").append(super.eventMessage).append('\'')
        sb.append(", instanceGroup='").append(instanceGroupName).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
