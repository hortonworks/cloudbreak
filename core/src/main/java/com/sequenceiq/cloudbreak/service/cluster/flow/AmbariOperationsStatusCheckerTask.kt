package com.sequenceiq.cloudbreak.service.cluster.flow

import java.math.BigDecimal
import java.util.Date

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.google.common.base.Optional
import com.sequenceiq.ambari.client.AmbariClient
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.ClusterBasedStatusCheckerTask
import com.sequenceiq.cloudbreak.service.cluster.AmbariOperationFailedException
import com.sequenceiq.cloudbreak.service.notification.Notification
import com.sequenceiq.cloudbreak.service.notification.NotificationSender

@Component
class AmbariOperationsStatusCheckerTask : ClusterBasedStatusCheckerTask<AmbariOperations>() {

    @Inject
    private val notificationSender: NotificationSender? = null

    override fun checkStatus(t: AmbariOperations): Boolean {
        val installRequests = t.requests
        var allFinished = true
        for (request in installRequests.entries) {
            val ambariClient = t.ambariClient
            val installProgress = Optional.fromNullable(ambariClient.getRequestProgress(request.value)).or(PENDING)
            LOGGER.info("Ambari operation: '{}', Progress: {}", request.key, installProgress)
            notificationSender!!.send(getAmbariProgressNotification(installProgress.toLong(), t.stack, t.ambariOperationType))
            allFinished = allFinished && COMPLETED.compareTo(installProgress) == 0
            if (FAILED.compareTo(installProgress) == 0) {
                var failed = true
                for (i in 0..MAX_RETRY - 1) {
                    if (ambariClient.getRequestProgress(request.value).compareTo(FAILED) != 0) {
                        failed = false
                        break
                    }
                }
                if (failed) {
                    notificationSender.send(getAmbariProgressNotification(java.lang.Long.parseLong("100"), t.stack, t.ambariOperationType))
                    throw AmbariOperationFailedException(String.format("Ambari operation failed: [component: '%s', requestID: '%s']", request.key,
                            request.value))
                }
            }
        }
        return allFinished
    }

    private fun getAmbariProgressNotification(progressValue: Long?, stack: Stack, ambariOperationType: AmbariOperationType): Notification {
        val notification = Notification()
        notification.eventType = ambariOperationType.name
        notification.eventTimestamp = Date()
        notification.eventMessage = progressValue.toString()
        notification.owner = stack.owner
        notification.account = stack.account
        notification.cloud = stack.cloudPlatform()
        notification.region = stack.region
        notification.stackId = stack.id
        notification.stackName = stack.name
        notification.stackStatus = stack.status
        if (stack.cluster != null) {
            notification.clusterId = stack.cluster.id
            notification.clusterName = stack.cluster.name
        }
        return notification
    }

    override fun handleTimeout(t: AmbariOperations) {
        throw IllegalStateException(String.format("Ambari operations timed out: %s", t.requests))
    }

    override fun successMessage(t: AmbariOperations): String {
        return String.format("Requested Ambari operations completed: %s", t.requests.toString())
    }

    override fun handleException(e: Exception) {
        LOGGER.error("Ambari operation failed.", e)
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(AmbariOperationsStatusCheckerTask::class.java)

        private val COMPLETED = BigDecimal(100.0)
        private val FAILED = BigDecimal(-1.0)
        private val PENDING = BigDecimal(0)
        private val MAX_RETRY = 3
    }

}
