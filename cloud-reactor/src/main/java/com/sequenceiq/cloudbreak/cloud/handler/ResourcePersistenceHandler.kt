package com.sequenceiq.cloudbreak.cloud.handler

import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.dao.TransientDataAccessException
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.notification.model.ResourceNotification
import com.sequenceiq.cloudbreak.cloud.notification.model.ResourcePersisted
import com.sequenceiq.cloudbreak.cloud.retry.ErrorTask
import com.sequenceiq.cloudbreak.cloud.retry.ExceptionCheckTask
import com.sequenceiq.cloudbreak.cloud.retry.RetryTask
import com.sequenceiq.cloudbreak.cloud.retry.RetryUtil
import com.sequenceiq.cloudbreak.cloud.service.Persister

import reactor.bus.Event
import reactor.fn.Consumer

@Component
class ResourcePersistenceHandler : Consumer<Event<ResourceNotification>> {

    @Inject
    private val cloudResourcePersisterService: Persister<ResourceNotification>? = null

    override fun accept(event: Event<ResourceNotification>) {
        LOGGER.info("Resource notification event received: {}", event)
        val notification = event.data

        RetryUtil.withDefaultRetries().retry {
            var notificationPersisted: ResourceNotification? = null
            when (notification.type) {
                ResourceNotificationType.CREATE -> notificationPersisted = cloudResourcePersisterService!!.persist(notification)
                ResourceNotificationType.UPDATE -> notificationPersisted = cloudResourcePersisterService!!.update(notification)
                ResourceNotificationType.DELETE -> notificationPersisted = cloudResourcePersisterService!!.delete(notification)
                else -> throw IllegalArgumentException("Unsupported notification type: " + notification.type)
            }
            notificationPersisted!!.promise.onNext(ResourcePersisted())
        }.checkIfRecoverable { e -> e is TransientDataAccessException }.ifNotRecoverable { e -> notification.promise.onError(e) }.run()
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ResourcePersistenceHandler::class.java)
    }
}
