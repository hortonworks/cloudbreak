package com.sequenceiq.cloudbreak.service.events

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent

interface CloudbreakEventService {
    fun fireCloudbreakEvent(stackId: Long?, eventType: String, eventMessage: String)

    fun fireCloudbreakInstanceGroupEvent(stackId: Long?, eventType: String, eventMessage: String, instanceGroupName: String)

    fun createStackEvent(eventData: CloudbreakEventData): CloudbreakEvent

    fun cloudbreakEvents(user: String, since: Long?): List<CloudbreakEvent>
}
