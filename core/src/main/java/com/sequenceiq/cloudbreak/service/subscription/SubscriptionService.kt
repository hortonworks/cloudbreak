package com.sequenceiq.cloudbreak.service.subscription

import javax.inject.Inject
import javax.transaction.Transactional

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import com.sequenceiq.cloudbreak.domain.Subscription
import com.sequenceiq.cloudbreak.repository.SubscriptionRepository

@Service
@Transactional
class SubscriptionService {

    @Inject
    private val subscriptionRepository: SubscriptionRepository? = null

    @Transactional(Transactional.TxType.NEVER)
    fun subscribe(subscription: Subscription): Long? {
        var exists: Subscription? = null
        val clientSubscriptions = subscriptionRepository!!.findByClientId(subscription.clientId)
        for (s in clientSubscriptions) {
            if (s.endpoint == subscription.endpoint) {
                exists = s
                LOGGER.info(String.format("Subscription already exists for this client with the same endpoint [client: '%s', endpoint: '%s']",
                        subscription.clientId,
                        subscription.endpoint))
                break
            }
        }
        return if (exists == null) subscriptionRepository.save(subscription).id else exists.id
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(SubscriptionService::class.java)
    }
}
