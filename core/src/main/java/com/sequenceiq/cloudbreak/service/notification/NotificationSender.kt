package com.sequenceiq.cloudbreak.service.notification

interface NotificationSender {

    fun send(notification: Notification)
}
