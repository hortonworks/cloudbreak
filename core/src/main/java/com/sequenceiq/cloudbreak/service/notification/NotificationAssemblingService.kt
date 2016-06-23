package com.sequenceiq.cloudbreak.service.notification

import javax.inject.Inject

import org.springframework.context.MessageSource
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent

@Component
class NotificationAssemblingService {
    @Inject
    private val messageSource: MessageSource? = null

    fun createNotification(cloudbreakEvent: CloudbreakEvent): Notification {
        val notification = Notification()
        notification.eventType = cloudbreakEvent.eventType
        notification.eventTimestamp = cloudbreakEvent.eventTimestamp
        notification.eventMessage = cloudbreakEvent.eventMessage
        notification.owner = cloudbreakEvent.owner
        notification.account = cloudbreakEvent.account
        notification.cloud = cloudbreakEvent.cloud
        notification.region = cloudbreakEvent.region
        notification.blueprintName = cloudbreakEvent.blueprintName
        notification.blueprintId = cloudbreakEvent.blueprintId
        notification.stackId = cloudbreakEvent.stackId
        notification.stackName = cloudbreakEvent.stackName
        notification.stackStatus = cloudbreakEvent.stackStatus
        notification.nodeCount = cloudbreakEvent.nodeCount
        notification.instanceGroup = cloudbreakEvent.instanceGroup
        notification.clusterStatus = cloudbreakEvent.clusterStatus
        notification.clusterId = cloudbreakEvent.clusterId
        notification.clusterName = cloudbreakEvent.clusterName
        return notification
    }

}
