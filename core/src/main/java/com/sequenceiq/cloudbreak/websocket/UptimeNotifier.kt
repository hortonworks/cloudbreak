package com.sequenceiq.cloudbreak.websocket

import java.util.Date

import javax.inject.Inject

import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.Cluster
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.repository.ClusterRepository
import com.sequenceiq.cloudbreak.repository.StackRepository
import com.sequenceiq.cloudbreak.service.notification.Notification
import com.sequenceiq.cloudbreak.service.notification.NotificationSender

@Component
class UptimeNotifier {

    @Inject
    private val clusterRepository: ClusterRepository? = null

    @Inject
    private val stackRepository: StackRepository? = null

    @Inject
    private val notificationSender: NotificationSender? = null


    @Scheduled(fixedDelay = 60000)
    fun sendUptime() {
        val clusters = clusterRepository!!.findAll() as List<Cluster>
        val now = Date().time
        for (cluster in clusters) {
            val stack = stackRepository!!.findStackForCluster(cluster.id)
            if (stack != null && !stack.isDeleteCompleted) {
                val uptime = if (cluster.upSince == null || !cluster.isAvailable) 0L else now - cluster.upSince!!
                val notification = createUptimeNotification(stack, uptime)
                notificationSender!!.send(notification)
            }
        }
    }

    private fun createUptimeNotification(stack: Stack, uptime: Long?): Notification {
        val notification = Notification()
        notification.owner = stack.owner
        notification.account = stack.account
        notification.stackId = stack.id
        notification.eventType = UPTIME_NOTIFICATION
        notification.eventMessage = uptime.toString()
        if (stack.credential == null) {
            notification.cloud = "null"
        } else {
            notification.cloud = stack.credential.cloudPlatform().toString()
        }
        if (stack.cluster == null || stack.cluster.blueprint == null) {
            notification.blueprintId = null
            notification.blueprintName = "null"
        } else {
            notification.blueprintId = stack.cluster.blueprint.id
            notification.blueprintName = stack.cluster.blueprint.blueprintName
            notification.clusterName = stack.cluster.name
            notification.clusterId = stack.cluster.id
        }
        return notification
    }

    companion object {
        private val UPTIME_NOTIFICATION = "UPTIME_NOTIFICATION"
    }
}
