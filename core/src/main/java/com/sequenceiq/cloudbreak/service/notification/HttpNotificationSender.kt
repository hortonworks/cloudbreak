package com.sequenceiq.cloudbreak.service.notification

import javax.inject.Inject
import javax.ws.rs.client.Client
import javax.ws.rs.client.Entity
import javax.ws.rs.core.HttpHeaders
import javax.ws.rs.core.MediaType

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.client.ConfigKey
import com.sequenceiq.cloudbreak.client.RestClientUtil
import com.sequenceiq.cloudbreak.domain.Subscription
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository

@Service
class HttpNotificationSender : NotificationSender {

    @Inject
    private val subscriptionRepository: SubscriptionRepository? = null

    private val restClient = RestClientUtil[ConfigKey(false, false)]

    override fun send(notification: Notification) {
        val subscriptions = subscriptionRepository!!.findAll() as List<Subscription>
        for (subscription in subscriptions) {
            val endpoint = subscription.endpoint
            try {
                restClient.target(endpoint).request().header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON).post<String>(Entity.json(notification), String::class.java)
            } catch (ex: Exception) {
                LOGGER.info("Could not send notification to the specified endpoint: '{}' Cause: {}", endpoint, ex.message)
            }

        }
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(HttpNotificationSender::class.java)
    }
}
