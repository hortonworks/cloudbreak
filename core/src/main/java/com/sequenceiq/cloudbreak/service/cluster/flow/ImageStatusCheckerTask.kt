package com.sequenceiq.cloudbreak.service.cluster.flow

import java.util.Date

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.common.type.ImageStatus
import com.sequenceiq.cloudbreak.common.type.ImageStatusResult
import com.sequenceiq.cloudbreak.domain.Stack
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask
import com.sequenceiq.cloudbreak.service.notification.Notification
import com.sequenceiq.cloudbreak.service.notification.NotificationSender
import com.sequenceiq.cloudbreak.service.stack.connector.adapter.ServiceProviderSetupAdapter

@Component
class ImageStatusCheckerTask : StackBasedStatusCheckerTask<ImageCheckerContext>() {

    @Inject
    private val provisioning: ServiceProviderSetupAdapter? = null

    @Inject
    private val notificationSender: NotificationSender? = null

    override fun checkStatus(t: ImageCheckerContext): Boolean {
        try {
            val imageStatusResult = provisioning!!.checkImage(t.stack)
            if (imageStatusResult.imageStatus == ImageStatus.CREATE_FAILED) {
                notificationSender!!.send(getImageCopyNotification(imageStatusResult, t.stack))
                throw CloudbreakServiceException("Image copy operation finished with failed status.")
            } else if (imageStatusResult.imageStatus == ImageStatus.CREATE_FINISHED) {
                notificationSender!!.send(getImageCopyNotification(imageStatusResult, t.stack))
                return true
            } else {
                notificationSender!!.send(getImageCopyNotification(imageStatusResult, t.stack))
                return false
            }
        } catch (e: Exception) {
            throw CloudbreakServiceException(e)
        }

    }

    private fun getImageCopyNotification(result: ImageStatusResult, stack: Stack): Notification {
        val notification = Notification()
        notification.eventType = "IMAGE_COPY_STATE"
        notification.eventTimestamp = Date()
        notification.eventMessage = result.statusProgressValue.toString()
        notification.owner = stack.owner
        notification.account = stack.account
        notification.cloud = stack.cloudPlatform().toString()
        notification.region = stack.region
        notification.stackId = stack.id
        notification.stackName = stack.name
        notification.stackStatus = stack.status
        return notification

    }

    override fun handleTimeout(t: ImageCheckerContext) {
        throw CloudbreakServiceException("Operation timed out. Image copy operation failed.")
    }

    override fun successMessage(t: ImageCheckerContext): String {
        return String.format("Image copy operation finished with success state.")
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(ImageStatusCheckerTask::class.java)
    }

}
