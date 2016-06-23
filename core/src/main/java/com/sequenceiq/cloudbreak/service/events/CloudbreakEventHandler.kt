package com.sequenceiq.cloudbreak.service.events


import javax.inject.Inject

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent
import com.sequenceiq.cloudbreak.logger.MDCBuilder
import com.sequenceiq.cloudbreak.service.notification.NotificationAssemblingService
import com.sequenceiq.cloudbreak.service.notification.NotificationSender

import reactor.bus.Event
import reactor.fn.Consumer

@Component
class CloudbreakEventHandler : Consumer<Event<CloudbreakEventData>> {

    @Inject
    private val eventService: CloudbreakEventService? = null

    @Inject
    private val notificationSender: NotificationSender? = null

    @Inject
    private val notificationAssemblingService: NotificationAssemblingService? = null

    override fun accept(cloudbreakEvent: Event<CloudbreakEventData>) {
        LOGGER.info("Handling cloudbreak event: {}", cloudbreakEvent)
        val event = cloudbreakEvent.data
        MDCBuilder.buildMdcContext(event)
        LOGGER.info("Persisting data: {}", event)
        val persistedEvent = eventService!!.createStackEvent(event)
        LOGGER.info("Sending notification with data: {}", persistedEvent)
        notificationSender!!.send(notificationAssemblingService!!.createNotification(persistedEvent))
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(CloudbreakEventHandler::class.java)
    }
}
