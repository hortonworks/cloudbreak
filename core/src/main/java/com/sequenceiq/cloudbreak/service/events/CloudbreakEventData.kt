package com.sequenceiq.cloudbreak.service.events

open class CloudbreakEventData(var entityId: Long?, var eventType: String?, var eventMessage: String?) {

    override fun toString(): String {
        val sb = StringBuilder("CloudbreakEventData{")
        sb.append("entityId=").append(entityId)
        sb.append(", eventType='").append(eventType).append('\'')
        sb.append(", eventMessage='").append(eventMessage).append('\'')
        sb.append('}')
        return sb.toString()
    }
}
